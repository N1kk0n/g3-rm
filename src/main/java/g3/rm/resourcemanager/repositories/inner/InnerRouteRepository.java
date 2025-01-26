package g3.rm.resourcemanager.repositories.inner;

import g3.rm.resourcemanager.dtos.RouteVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InnerRouteRepository {
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public InnerRouteRepository(@Qualifier("innerJdbcTemplate") NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public RouteVertex getFirstVertex(int graphId) {
        String sql = """
            select operation_name,
                   component_name,
                   topic_name
            from graph g left join vertex v on v.graph_id = g.id
                         left join operation op on op.id = v.operation_id
                         left join component comp on comp.id = op.component_id
            where graph_id = :graphId and
                  vertex_num = 1
            """;
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("graphId", graphId);
        return template.queryForObject(sql, sqlParameterSource, (rs, rowNum) -> {
            RouteVertex rv = new RouteVertex();
            rv.setGraph_id(graphId);
            rv.setOperation(rs.getString("operation_name"));
            rv.setConsumer(rs.getString("component_name"));
            rv.setTopic(rs.getString("topic_name"));
            return rv;
        });
    }

    public RouteVertex route(long graphId, String operation, int result) {
        String sql = """
            select next_v.graph_id as next_graph_id,
                   next_op.operation_name as next_operation_name,
                   next_comp.component_name as next_component_name,
                   next_comp.topic_name as next_topic_name
            from graph g left join edge e on e.graph_id = g.id
                     left join vertex curr_v on e.curr_vertex_id = curr_v.id
                     left join operation curr_op on curr_op.id = curr_v.operation_id
                     left join component curr_comp on curr_comp.id = curr_op.component_id
                     left join vertex next_v on e.next_vertex_id = next_v.id
                     left join operation next_op on next_op.id = next_v.operation_id
                     left join component next_comp on next_comp.id = next_op.component_id
            where g.id = :graphId and
                  curr_op.operation_name = :operation and
                  curr_comp.component_name='rm' and
                  result = :result;
            """;
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("graphId", graphId)
                .addValue("operation", operation)
                .addValue("result", result);
        return template.queryForObject(sql, sqlParameterSource, (rs, rowNum) -> {
            RouteVertex rv = new RouteVertex();
            rv.setGraph_id(rs.getInt("next_graph_id"));
            rv.setOperation(rs.getString("next_operation_name"));
            rv.setConsumer(rs.getString("next_component_name"));
            rv.setTopic(rs.getString("next_topic_name"));
            return rv;
        });
    }
}
