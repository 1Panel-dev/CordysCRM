export const enum IRNodeType {
  Literal = 'literal', // string|number|boolean
  Field = 'field', // 字段
  Binary = 'binary', // 运算符
  Compare = 'compare', // 比较运算符
  Function = 'function', // 函数
  Invalid = 'invalid' // 无效节点
}