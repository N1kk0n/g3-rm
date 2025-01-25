package g3.rm.resourcemanager.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class NextRouteVertex {
    String consumer;
    String operation;
}
