import evaluateIR from './evaluator';
import registerBuiltinFunctions from './functions';

registerBuiltinFunctions();

export default evaluateIR;
