package io.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jodd.util.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseTreeNode {

    @Schema(description = "节点ID")
    private String id;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "父节点ID")
    private String parentId;

    @Schema(description = "组织id")
    private String organizationId;

    @Schema(description = "子节点")
    private List<BaseTreeNode> children = new ArrayList<>();

    public BaseTreeNode(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public BaseTreeNode(String id, String name, String parentId) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
    }

    public void addChild(BaseTreeNode node) {
        node.setParentId(this.getId());
        children.add(node);
    }

    public static <T extends BaseTreeNode> List<T> buildTree(List<T> nodeList) {
        // 用于存储节点的 Map，key 是节点 ID
        Map<String, T> nodeMap = new HashMap<>();
        // 用于存储最终的根节点列表
        List<T> rootNodes = new ArrayList<>();

        // 1. 将所有节点放入 Map 中
        for (T node : nodeList) {
            nodeMap.put(node.getId(), node);
        }

        // 2. 遍历节点列表，构建父子关系
        for (T node : nodeList) {
            if (StringUtil.isBlank(node.getParentId()) || Strings.CI.equals(node.getParentId(), "NONE")) {
                // 没有父节点，则为根节点
                rootNodes.add(node);
            } else {
                // 获取父节点
                T parentNode = nodeMap.get(node.getParentId());
                if (parentNode != null) {
                    // 将当前节点添加到父节点的子节点列表中
                    parentNode.addChild(node);
                }
            }
        }

        // 3. 返回根节点列表
        return rootNodes;
    }

}
