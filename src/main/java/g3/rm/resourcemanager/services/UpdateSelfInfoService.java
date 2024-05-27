package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.repositories.UpdateSelfInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UpdateSelfInfoService {
    @Autowired
    UpdateSelfInfoRepository updateSelfInfoRepository;

    public void setManagerOnlineStatus(String managerName) {
        updateSelfInfoRepository.setManagerOnlineStatus(managerName, true);
    }
}
