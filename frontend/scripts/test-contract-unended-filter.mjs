import { createJiti } from 'jiti';
import assert from 'node:assert/strict';

const jiti = createJiti(import.meta.url);
const {
  CONTRACT_END_TIME_FIELD,
  hasContractEndTimeCondition,
  hasContractEndTimeFilter,
  withDefaultUnendedContractFilter,
  withDefaultUnendedContractFilters,
} = jiti('../packages/web/src/views/contract/contract/defaultUnendedContractFilter.ts');

const fixedNow = 1779200000000;

const defaultFilter = withDefaultUnendedContractFilter(undefined, fixedNow);
assert.equal(defaultFilter.searchMode, 'AND');
assert.deepEqual(defaultFilter.conditions, [
  {
    name: CONTRACT_END_TIME_FIELD,
    value: fixedNow,
    operator: 'GE',
    multipleValue: false,
    type: 'DATE_TIME',
  },
]);

const existingFilter = {
  searchMode: 'AND',
  conditions: [
    {
      name: 'customerId',
      value: 'customer-1',
      operator: 'EQUALS',
      multipleValue: false,
      type: 'DATA_SOURCE',
    },
  ],
};
const mergedFilter = withDefaultUnendedContractFilter(existingFilter, fixedNow);
assert.equal(mergedFilter.conditions.length, 2);
assert.equal(mergedFilter.conditions[0].name, 'customerId');
assert.equal(mergedFilter.conditions[1].name, CONTRACT_END_TIME_FIELD);
assert.equal(mergedFilter.conditions[1].operator, 'GE');

const userEndTimeFilter = {
  searchMode: 'OR',
  conditions: [
    {
      name: CONTRACT_END_TIME_FIELD,
      value: [fixedNow - 1000, fixedNow + 1000],
      operator: 'BETWEEN',
      multipleValue: true,
      type: 'DATE_TIME',
    },
  ],
};
const preservedFilter = withDefaultUnendedContractFilter(userEndTimeFilter, fixedNow);
assert.equal(preservedFilter.searchMode, 'OR');
assert.deepEqual(preservedFilter.conditions, userEndTimeFilter.conditions);
assert.equal(preservedFilter.conditions.filter((item) => item.name === CONTRACT_END_TIME_FIELD).length, 1);

assert.equal(hasContractEndTimeCondition(userEndTimeFilter), true);
assert.equal(hasContractEndTimeCondition(existingFilter), false);

for (const filter of [defaultFilter, mergedFilter, preservedFilter]) {
  assert.equal(
    filter.conditions.some((item) => item.name === 'startTime'),
    false,
    'default contract filter must not inject startTime'
  );
}

const defaultFilters = withDefaultUnendedContractFilters([], fixedNow);
assert.deepEqual(defaultFilters, [
  {
    name: CONTRACT_END_TIME_FIELD,
    value: fixedNow,
    operator: 'GE',
    multipleValue: false,
  },
]);

const tableFilters = [
  {
    name: 'stage',
    value: 'active',
    operator: 'EQUALS',
    multipleValue: false,
  },
];
assert.deepEqual(withDefaultUnendedContractFilters(tableFilters, fixedNow), [...tableFilters, ...defaultFilters]);

const existingEndTimeFilters = [
  {
    name: CONTRACT_END_TIME_FIELD,
    value: fixedNow - 1000,
    operator: 'GT',
    multipleValue: false,
  },
];
assert.strictEqual(withDefaultUnendedContractFilters(existingEndTimeFilters, fixedNow), existingEndTimeFilters);
assert.equal(hasContractEndTimeFilter(existingEndTimeFilters), true);
assert.equal(hasContractEndTimeFilter(tableFilters), false);
assert.equal(hasContractEndTimeFilter([{ dataIndex: CONTRACT_END_TIME_FIELD }]), true);
assert.equal(
  hasContractEndTimeCondition({
    searchMode: 'AND',
    conditions: [{ dataIndex: CONTRACT_END_TIME_FIELD, value: fixedNow, operator: 'GE', multipleValue: false }],
  }),
  true
);

const userOrFilter = {
  searchMode: 'OR',
  conditions: [
    {
      name: 'customerId',
      value: 'customer-2',
      operator: 'EQUALS',
      multipleValue: false,
      type: 'DATA_SOURCE',
    },
  ],
};
const requestBodyShape = {
  combineSearch: userOrFilter,
  filters: withDefaultUnendedContractFilters([], fixedNow),
};
assert.equal(requestBodyShape.combineSearch.searchMode, 'OR');
assert.equal(requestBodyShape.filters[0].name, CONTRACT_END_TIME_FIELD);

console.log('contract unended default filter tests passed');
