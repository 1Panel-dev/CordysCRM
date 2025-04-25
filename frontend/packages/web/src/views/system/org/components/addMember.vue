<template>
  <CrmDrawer
    v-model:show="showDrawer"
    :width="480"
    :title="form.id ? t('org.updateMember') : t('org.addMember')"
    :ok-text="t('common.update')"
    @cancel="cancelHandler"
  >
    <div class="mr-[20%]">
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="left"
        :label-width="100"
        require-mark-placement="left"
      >
        <n-form-item require-mark-placement="left" label-placement="left" path="name" :label="t('org.userName')">
          <n-input v-model:value="form.name" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item require-mark-placement="left" label-placement="left" path="gender" :label="t('org.gender')">
          <n-radio-group v-model:value="form.gender" name="radiogroup">
            <n-space>
              <n-radio key="male" :value="false">
                {{ t('org.male') }}
              </n-radio>
              <n-radio key="female" :value="true">
                {{ t('org.female') }}
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
        <n-form-item require-mark-placement="left" label-placement="left" path="phone" :label="t('common.phoneNumber')">
          <n-input v-model:value="form.phone" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item require-mark-placement="left" label-placement="left" path="email" :label="t('org.userEmail')">
          <n-input v-model:value="form.email" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="departmentId"
          :label="t('org.department')"
        >
          <n-tree-select
            v-model:value="form.departmentId"
            :options="department"
            label-field="name"
            key-field="id"
            filterable
            clearable
            children-field="children"
          >
            <template #empty>
              <div class="flex w-full items-center justify-start text-[var(--text-n4)]">
                {{ t('org.noDepartmentToChoose') }}
              </div>
            </template>
            <!-- TODO xxw -->
            <!-- <template #action>
              <div class="text-left">
                <n-button type="primary" text>
                  <template #icon>
                    <CrmIcon type="iconicon_add" :size="16" class="text-[var(--primary-8)]" />
                  </template>
                  {{ t('org.addDepartment') }}
                </n-button>
              </div>
            </template> -->
          </n-tree-select>
        </n-form-item>
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="employeeId"
          :label="t('org.employeeNumber')"
        >
          <n-input v-model:value="form.employeeId" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <CrmExpandButton v-model:expand="showForm">
          <n-form-item
            require-mark-placement="left"
            label-placement="left"
            path="employeeType"
            :label="t('org.employeeType')"
          >
            <n-select
              v-model:value="form.employeeType"
              :placeholder="t('common.pleaseSelect')"
              :options="employeeTypeOptions"
            />
          </n-form-item>
          <n-form-item
            require-mark-placement="left"
            label-placement="left"
            path="supervisorId"
            :label="t('org.directSuperior')"
          >
            <CrmUserSelect
              ref="crmUserSelectRef"
              v-model:value="form.supervisorId"
              value-field="id"
              label-field="name"
              mode="remote"
              filterable
              :fetch-api="getUserOptions"
            />
          </n-form-item>
          <n-form-item require-mark-placement="left" label-placement="left" path="position" :label="t('org.position')">
            <n-input v-model:value="form.position" type="text" :placeholder="t('common.pleaseInput')" />
          </n-form-item>
          <n-form-item
            require-mark-placement="left"
            label-placement="left"
            path="workCity"
            :label="t('org.workingCity')"
          >
            <CrmCitySelect v-model:value="form.workCity" />
          </n-form-item>
          <n-form-item require-mark-placement="left" label-placement="left" path="roleIds" :label="t('org.role')">
            <n-select
              v-model:value="form.roleIds"
              multiple
              filterable
              :placeholder="t('common.pleaseSelect')"
              :options="roleOptions"
            />
          </n-form-item>
          <!-- TODO  不上 -->
          <!-- <n-form-item
            require-mark-placement="left"
            label-placement="left"
            path="userGroupIds"
            :label="t('org.userGroup')"
          >
            <n-select
              v-model:value="form.userGroupIds"
              :placeholder="t('common.pleaseSelect')"
              :options="userGroupOptions"
            />
          </n-form-item> -->
        </CrmExpandButton>
      </n-form>
    </div>
    <template #footer>
      <div class="flex w-full items-center justify-between">
        <div class="ml-[4px] flex items-center gap-[8px]">
          <n-switch v-model:value="form.enable" /> {{ t('common.status') }}
        </div>
        <div class="flex items-center justify-end gap-[12px]">
          <n-button :disabled="loading" secondary @click="cancelHandler">
            {{ t('common.cancel') }}
          </n-button>
          <n-button
            v-if="!form.id"
            :loading="loading"
            type="primary"
            ghost
            class="n-btn-outline-primary"
            @click="handleSave(true)"
          >
            {{ t('common.saveAndContinue') }}
          </n-button>
          <n-button :loading="loading" type="primary" @click="handleSave(false)">
            {{ t('common.confirm') }}
          </n-button>
        </div>
      </div>
    </template>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import {
    FormInst,
    FormItemRule,
    FormRules,
    NButton,
    NForm,
    NFormItem,
    NInput,
    NRadio,
    NRadioGroup,
    NSelect,
    NSpace,
    NSwitch,
    NTreeSelect,
    SelectOption,
    useMessage,
  } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { validateEmail, validatePhone } from '@lib/shared/method/validate';
  import type { MemberParams } from '@lib/shared/models/system/org';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import type { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import CrmCitySelect from '@/components/business/crm-city-select/index.vue';
  import CrmExpandButton from '@/components/business/crm-expand-button/index.vue';
  import CrmUserSelect from '@/components/business/crm-user-select/index.vue';

  import { addUser, getDepartmentTree, getRoleOptions, getUserDetail, getUserOptions, updateUser } from '@/api/modules';

  const Message = useMessage();
  const { t } = useI18n();

  const emit = defineEmits<{
    (e: 'brash'): void;
    (e: 'close'): void;
  }>();

  const props = defineProps<{
    userId: string;
  }>();

  const showDrawer = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const initUserForm: MemberParams = {
    name: '',
    gender: false,
    phone: '',
    email: '',
    departmentId: '',
    employeeId: '',
    position: '',
    enable: true,
    employeeType: null,
    workCity: null,
    supervisorId: null,
    roleIds: [],
    userGroupIds: [],
    userName: '',
    roles: [],
  };

  const form = ref<MemberParams>(cloneDeep(initUserForm));

  function validateUserEmail(rule: FormItemRule, value: string) {
    if (!value) {
      return new Error(t('common.notNull', { value: `${t('org.userEmail')}` }));
    }
    if (!validateEmail(value)) {
      return new Error(t('common.emailErrTip'));
    }
    return true;
  }

  function validateUserPhone(rule: FormItemRule, value: string) {
    if (!value) {
      return new Error(t('common.notNull', { value: `${t('common.phoneNumber')}` }));
    }
    if (!validatePhone(value)) {
      return new Error(t('common.userPhoneErrTip'));
    }
    return true;
  }

  const rules: FormRules = {
    name: [
      { required: true, message: t('common.notNull', { value: `${t('org.userName')}` }), trigger: ['input', 'blur'] },
    ],
    phone: [{ required: true, validator: validateUserPhone, trigger: ['input', 'blur'] }],
    email: [{ required: true, validator: validateUserEmail, trigger: ['input', 'blur'] }],
    departmentId: [{ required: true, message: t('common.pleaseSelect'), trigger: ['input', 'blur'] }],
  };

  const showForm = ref(false);

  const employeeTypeOptions = ref([
    {
      value: 'formal',
      label: t('org.formalUser'),
    },
    {
      value: 'internship',
      label: t('org.internshipUser'),
    },
    {
      value: 'outsourcing',
      label: t('org.outsourcingUser'),
    },
  ]);

  const userGroupOptions = ref([]);

  const loading = ref(false);
  const formRef = ref<FormInst | null>(null);

  function cancelHandler() {
    form.value = cloneDeep(initUserForm);
    emit('close');
  }

  const crmUserSelectRef = ref<InstanceType<typeof CrmUserSelect>>();

  function handleSave(isContinue: boolean) {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          if (form.value.id) {
            await updateUser(form.value);
            Message.success(t('common.updateSuccess'));
          } else {
            await addUser(form.value);
            Message.success(t('common.addSuccess'));
          }

          if (isContinue) {
            form.value = cloneDeep(initUserForm);
            crmUserSelectRef.value?.loadUsers();
          } else {
            cancelHandler();
          }
          emit('brash');
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          loading.value = false;
        }
      }
    });
  }

  const roleOptions = ref<SelectOption[]>([]);
  async function initRoleList() {
    try {
      const res = await getRoleOptions();
      roleOptions.value = res.map((e: { id: string; name: string }) => {
        return { label: e.name, value: e.id };
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const department = ref<CrmTreeNodeData[]>([]);
  async function initDepartList() {
    try {
      department.value = await getDepartmentTree();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function getDetail() {
    try {
      if (props.userId) {
        const detail = await getUserDetail(props.userId);
        form.value = {
          ...detail,
          name: detail.userName,
          roleIds: detail.roles.map((e) => e.id),
        };
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => showDrawer.value,
    (val) => {
      if (val) {
        getDetail();
        initDepartList();
        initRoleList();
      }
    }
  );
</script>

<style scoped></style>
