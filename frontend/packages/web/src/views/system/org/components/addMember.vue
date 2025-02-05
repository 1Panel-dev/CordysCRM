<template>
  <CrmDrawer v-model:show="showDrawer" :width="480" :title="t('org.addMember')" :ok-text="t('common.update')">
    <div class="mr-[20%]">
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="left"
        label-width="auto"
        require-mark-placement="left"
      >
        <n-form-item require-mark-placement="left" label-placement="left" path="name" :label="t('org.userName')">
          <n-input v-model:value="form.name" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item require-mark-placement="left" label-placement="left" path="gender" :label="t('org.gender')">
          <n-radio-group v-model:value="form.gender" name="radiogroup">
            <n-space>
              <n-radio key="male" value="male">
                {{ t('org.male') }}
              </n-radio>
              <n-radio key="female" value="female">
                {{ t('org.female') }}
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="phoneNumber"
          :label="t('org.phoneNumber')"
        >
          <n-input v-model:value="form.phoneNumber" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item require-mark-placement="left" label-placement="left" path="email" :label="t('org.userEmail')">
          <n-input v-model:value="form.email" type="text" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="department"
          :label="t('org.department')"
        >
          <n-select v-model:value="form.department" :placeholder="t('common.pleaseSelect')" :options="department">
            <template #empty>
              <div class="flex w-full items-center justify-start text-[var(--text-n4)]">
                {{ t('org.noDepartmentToChoose') }}
              </div>
            </template>
            <template #action>
              <div class="text-left">
                <n-button type="primary" text @click="addDepartment">
                  <template #icon>
                    <CrmIcon type="iconicon_add" :size="16" class="text-[var(--primary-8)]" />
                  </template>
                  {{ t('org.addDepartment') }}
                </n-button>
              </div>
            </template>
          </n-select>
        </n-form-item>
        <n-form-item
          require-mark-placement="left"
          label-placement="left"
          path="employeeNumber"
          :label="t('org.employeeNumber')"
        >
          <n-input v-model:value="form.employeeNumber" type="text" :placeholder="t('common.pleaseInput')" />
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
            path="directSuperior"
            :label="t('org.directSuperior')"
          >
            <n-select
              v-model:value="form.directSuperior"
              :placeholder="t('common.pleaseSelect')"
              :options="superiorOptions"
            />
          </n-form-item>
          <n-form-item
            require-mark-placement="left"
            label-placement="left"
            path="workingCity"
            :label="t('org.workingCity')"
          >
            <n-select
              v-model:value="form.workingCity"
              :placeholder="t('common.pleaseSelect')"
              :options="workingCityOptions"
            />
          </n-form-item>
          <n-form-item require-mark-placement="left" label-placement="left" path="role" :label="t('org.role')">
            <n-select v-model:value="form.role" :placeholder="t('common.pleaseSelect')" :options="roleOptions" />
          </n-form-item>
          <n-form-item
            require-mark-placement="left"
            label-placement="left"
            path="userGroup"
            :label="t('org.userGroup')"
          >
            <n-select
              v-model:value="form.userGroup"
              :placeholder="t('common.pleaseSelect')"
              :options="userGroupOptions"
            />
          </n-form-item>
        </CrmExpandButton>
      </n-form>
    </div>
    <template #footer>
      <div class="flex w-full items-center justify-between">
        <div class="ml-[4px] flex items-center gap-[8px]">
          <n-switch v-model:value="form.status" /> {{ t('common.status') }}
        </div>
        <div>
          <n-button :disabled="loading" secondary @click="cancelHandler">
            {{ t('common.cancel') }}
          </n-button>
          <n-button class="mx-[12px]" :loading="loading" type="tertiary" @click="continueAdd">
            {{ t('common.saveAndContinue') }}
          </n-button>
          <n-button :loading="loading" type="primary" @click="confirmHandler">
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
    useMessage,
  } from 'naive-ui';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmExpandButton from '@/components/business/crm-expand-button/index.vue';

  import { useI18n } from '@/hooks/useI18n';
  import { validateEmail, validatePhone } from '@/utils/validate';

  const Message = useMessage();

  const { t } = useI18n();

  const emit = defineEmits<{
    (e: 'addSuccess'): void;
  }>();

  const showDrawer = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const form = ref({
    name: '',
    gender: 'male',
    phoneNumber: '',
    email: '',
    department: '',
    employeeNumber: '',
    position: '',
    status: true,
    employeeType: undefined,
    workingCity: undefined,
    directSuperior: undefined,
    role: undefined,
    userGroup: undefined,
  });

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
      return new Error(t('common.notNull', { value: `${t('org.phoneNumber')}` }));
    }
    if (!validatePhone(value)) {
      return new Error(t('common.userPhoneErrTip'));
    }
    return true;
  }

  const rules: FormRules = {
    name: [{ required: true, message: t('common.notNull', { value: `${t('org.userName')}` }) }],
    phoneNumber: [{ validator: validateUserPhone, trigger: ['input', 'blur'] }],
    email: [{ validator: validateUserEmail, trigger: ['input', 'blur'] }],
    department: [{ required: true, message: t('common.pleaseSelect') }],
  };

  const department = ref([]);

  const showForm = ref(false);

  const employeeTypeOptions = ref([]);

  const roleOptions = ref([]);

  const userGroupOptions = ref([]);

  const superiorOptions = ref([]);

  const workingCityOptions = ref([]);
  const loading = ref(false);
  const formRef = ref<FormInst | null>(null);

  function cancelHandler() {
    showDrawer.value = false;
  }

  function confirmHandler() {
    formRef.value?.validate((error) => {
      if (!error) {
        try {
          loading.value = true;
          Message.success(t('common.updateSuccess'));
        } catch (e) {
          console.log(e);
        }
      }
    });
  }

  function continueAdd() {}

  function addDepartment() {
    emit('addSuccess');
  }
</script>

<style scoped></style>
