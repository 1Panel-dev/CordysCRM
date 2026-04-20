import { ActionNode, AddNode, ConditionBranchNode, EndNode, StartNode } from '../components/nodes';
import { Graph } from '@antv/x6';
import { register as registerVueShape } from '@antv/x6-vue-shape';

let registered = false;

export default function registerFlowNodes(): void {
  if (registered) {
    return;
  }

  registerVueShape({
    shape: 'flow-start-node',
    component: StartNode,
  });

  registerVueShape({
    shape: 'flow-action-node',
    component: ActionNode,
  });

  registerVueShape({
    shape: 'flow-condition-branch-node',
    component: ConditionBranchNode,
  });

  registerVueShape({
    shape: 'flow-end-node',
    component: EndNode,
  });

  registerVueShape({
    shape: 'flow-add-node',
    component: AddNode,
  });

  Graph.registerNode(
    'flow-add-condition-node',
    {
      inherit: 'rect',
    },
    true
  );

  registered = true;
}
