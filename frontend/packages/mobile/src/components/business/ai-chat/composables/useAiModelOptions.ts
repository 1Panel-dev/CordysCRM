import { createAiModelOptions } from '@lib/shared/ai-chat';

import { getAgentModelOptions } from '@/api/modules';

export default createAiModelOptions(getAgentModelOptions);
