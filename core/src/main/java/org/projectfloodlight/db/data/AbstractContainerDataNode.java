package org.projectfloodlight.db.data;

import org.projectfloodlight.db.BigDBException;
import org.projectfloodlight.db.expression.LocationPathExpression;
import org.projectfloodlight.db.query.Step;
import org.projectfloodlight.db.schema.SchemaNode;

public abstract class AbstractContainerDataNode extends AbstractDictionaryDataNode
        implements ContainerDataNode {

    @Override
    public NodeType getNodeType() {
        return NodeType.CONTAINER;
    }

    @Override
    protected Iterable<DataNodeWithPath> queryWithPath(SchemaNode schemaNode,
            LocationPathExpression queryPath, boolean expandTrailingList,
            boolean includeEmptyContainers) throws BigDBException {
        // Override to check that the step in the query path corresponding to
        // the container node doesn't contain any predicates, which aren't
        // allowed for container nodes, but are allowed for list element nodes,
        // i.e. index predicate.
        if (!queryPath.isAbsolute() && (queryPath.size() > 0)) {
            Step thisStep = queryPath.getStep(0);
            if (!thisStep.getPredicates().isEmpty()) {
                throw new BigDBException(
                        "Invalid predicate for container data node");
            }
        }
        return super.queryWithPath(schemaNode, queryPath, expandTrailingList,
                includeEmptyContainers);
    }
}
