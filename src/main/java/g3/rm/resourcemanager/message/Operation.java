package g3.rm.resourcemanager.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Operation {
    private Long route_id;
    private String name;
    private Integer result;
}
