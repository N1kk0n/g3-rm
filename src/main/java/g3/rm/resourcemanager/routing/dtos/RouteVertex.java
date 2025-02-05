package g3.rm.resourcemanager.routing.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class RouteVertex {
    int graph_id;
    String operation;
    String consumer;
    String topic;
}
