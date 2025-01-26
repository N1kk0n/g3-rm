package g3.rm.resourcemanager.repositories;

import g3.rm.resourcemanager.repositories.state.ComponentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class AutorunRepository {

    @Value("${server.port}")
    private int serverPort;
    private final String SELF_NAME = "rm";

    private final JdbcTemplate innerTemplate;
    private final JdbcTemplate stateTemplate;
    private final ComponentRepository componentRepository;

    public AutorunRepository(@Qualifier("innerDataSource") DataSource innerDataSource,
                             @Qualifier("stateDataSource") DataSource stateDataSource,
                             ComponentRepository componentRepository) {
        this.innerTemplate = new JdbcTemplate(innerDataSource);
        this.stateTemplate = new JdbcTemplate(stateDataSource);
        this.componentRepository = componentRepository;
    }

    public void initSelfRepository() {
        setSelfComponentParams();
        createInnerTables();
        setDataToInnerTables();
    }

    private void createInnerTables() {
        innerTemplate.execute("create table if not exists QUEUE_MANAGER_PARAM(ID identity primary key, PARAM_NAME varchar(32) unique, PARAM_VALUE varchar(256))");
        innerTemplate.execute("insert into QUEUE_MANAGER_PARAM(PARAM_NAME, PARAM_VALUE) values('UPDATE_DECISION_FLAG', 'false')");

        innerTemplate.execute("create table if not exists COMPONENT(ID int primary key, COMPONENT_NAME varchar(64), TOPIC_NAME varchar(64))");
        innerTemplate.execute("create table if not exists OPERATION(ID int primary key, OPERATION_NAME varchar(64), COMPONENT_ID int)");
        innerTemplate.execute("create table if not exists GRAPH(ID int primary key, GRAPH_NAME varchar(64) not null)");
        innerTemplate.execute("create table if not exists VERTEX(ID int primary key, GRAPH_ID int not null references GRAPH(ID), VERTEX_NUM int not null, OPERATION_ID int not null)");
        innerTemplate.execute("create table if not exists EDGE(GRAPH_ID int not null references GRAPH(id), CURR_VERTEX_ID int not null, RESULT int not null, NEXT_GRAPH_ID int not null, NEXT_VERTEX_ID int not null)");
    }

    private void setDataToInnerTables() {
        setComponentData();
        setOperationData();
        setGraphData();
        setVertexData();
        setEdgeData();
    }

    private void setComponentData() {
        record ComponentData(int id, String component_name, String topic_name) {}

        List<ComponentData> componentDataList = stateTemplate.query("select * from state_schema.component",
                (rs, rowNum) -> new ComponentData(rs.getInt("ID"),
                        rs.getString("COMPONENT_NAME"),
                        rs.getString("TOPIC_NAME")));
        String componentInsertSql = """
            insert into component(id, component_name, topic_name) values (?, ?, ?)
        """;
        innerTemplate.batchUpdate(componentInsertSql,
                componentDataList,
                componentDataList.size(),
                (PreparedStatement ps, ComponentData cd) -> {
                    ps.setInt(1, cd.id());
                    ps.setString(2, cd.component_name());
                    ps.setString(3, cd.topic_name());
                });
    }

    private void setOperationData() {
        record OperationData(int id, String operation_name, int component_id) {}

        List<OperationData> operationDataList = stateTemplate.query("select * from state_schema.operation",
                (rs, rowNum) -> new OperationData(rs.getInt("ID"),
                        rs.getString("OPERATION_NAME"),
                        rs.getInt("COMPONENT_ID")));
        String operationInsertSql = """
            insert into operation(id, operation_name, component_id) values (?, ?, ?)
        """;
        innerTemplate.batchUpdate(operationInsertSql,
                operationDataList,
                operationDataList.size(),
                (PreparedStatement ps, OperationData od) -> {
                    ps.setInt(1, od.id());
                    ps.setString(2, od.operation_name());
                    ps.setInt(3, od.component_id());
                });
    }

    private void setGraphData() {
        record GraphData(int id, String graph_name) {}

        List<GraphData> graphDataList = stateTemplate.query("select * from state_schema.graph",
                (rs, rowNum) -> new GraphData(rs.getInt("ID"), rs.getString("GRAPH_NAME")));
        String graphInsertSql = """
            insert into graph(id, graph_name) values (?, ?)
        """;
        innerTemplate.batchUpdate(graphInsertSql,
                graphDataList,
                graphDataList.size(),
                (PreparedStatement ps, GraphData gd) -> {
                    ps.setInt(1, gd.id());
                    ps.setString(2, gd.graph_name());
                });
    }

    private void setVertexData() {
        record VertexData(int id, int graph_id, int vertex_num, int operation_id) {}

        List<VertexData> vertexDataList = stateTemplate.query("select * from state_schema.vertex",
                (rs, rowNum) -> new VertexData(rs.getInt("ID"), rs.getInt("GRAPH_ID"), rs.getInt("VERTEX_NUM") ,rs.getInt("OPERATION_ID")));
        String vertexInsertSql = """
            insert into vertex(id, graph_id, vertex_num, operation_id) values (?, ?, ?, ?)
        """;
        innerTemplate.batchUpdate(vertexInsertSql,
                vertexDataList,
                vertexDataList.size(),
                (PreparedStatement ps, VertexData vd) -> {
                    ps.setInt(1, vd.id());
                    ps.setInt(2, vd.graph_id());
                    ps.setInt(3, vd.vertex_num());
                    ps.setInt(4, vd.operation_id());
                });
    }

    private void setEdgeData() {
        record EdgeData(int graph_id, int curr_vertex_id, int result, int next_graph_id, int next_vertex_id) {}

        List<EdgeData> edgeDataList = stateTemplate.query("select * from state_schema.edge",
                (rs, rowNum) -> new EdgeData(rs.getInt("GRAPH_ID"),
                        rs.getInt("CURR_VERTEX_ID"),
                        rs.getInt("RESULT"),
                        rs.getInt("NEXT_GRAPH_ID"),
                        rs.getInt("NEXT_VERTEX_ID")));
        String edgeInsertSql = """
            insert into edge(graph_id, curr_vertex_id, result, next_graph_id, next_vertex_id)
            values (?, ?, ?, ?, ?)
        """;
        innerTemplate.batchUpdate(edgeInsertSql,
                edgeDataList,
                edgeDataList.size(),
                (PreparedStatement ps, EdgeData ed) -> {
                    ps.setInt(1, ed.graph_id());
                    ps.setInt(2, ed.curr_vertex_id());
                    ps.setInt(3, ed.result());
                    ps.setInt(4, ed.next_graph_id());
                    ps.setInt(5, ed.next_vertex_id());
                });
    }

    private void setSelfComponentParams() {
        try {
            String hostAddress = Inet4Address.getLocalHost().getHostAddress();
            componentRepository.updateComponentAddress(SELF_NAME, hostAddress + ":" + serverPort);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
