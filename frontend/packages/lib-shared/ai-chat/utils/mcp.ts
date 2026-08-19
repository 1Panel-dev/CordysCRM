import type { AiChatMcp } from '../types';

export interface MatchedMcp {
  mcp: AiChatMcp;
  length: number;
}

export function getMcpReferenceText(mcp: AiChatMcp): string {
  return `[[${mcp.name}:${mcp.id}]]`;
}

function readMcpReference(text: string, index: number): AiChatMcp | undefined {
  const matched = text.slice(index).match(/^\[\[([^:\]]+):([^\]]+)]]/);

  return matched
    ? {
        name: matched[1],
        id: matched[2],
      }
    : undefined;
}

export function getMatchedMcp(text: string, index: number, mcps: AiChatMcp[]): MatchedMcp | undefined {
  const referenceMcp = readMcpReference(text, index);

  if (referenceMcp) {
    const matchedMcp = mcps.find((mcp) => mcp.id === referenceMcp.id) ?? referenceMcp;
    return {
      mcp: matchedMcp,
      length: getMcpReferenceText(referenceMcp).length,
    };
  }

  const nameMatchedMcp = mcps.find((mcp) => text.startsWith(mcp.name, index));

  return nameMatchedMcp
    ? {
        mcp: nameMatchedMcp,
        length: nameMatchedMcp.name.length,
      }
    : undefined;
}
