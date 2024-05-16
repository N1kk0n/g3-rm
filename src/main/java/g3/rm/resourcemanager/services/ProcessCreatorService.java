package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.entities.DeviceParam;
import g3.rm.resourcemanager.processes.*;
import g3.rm.resourcemanager.repositories.DeviceParamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import g3.rm.resourcemanager.dtos.TaskObject;

@Service
public class ProcessCreatorService {
    @Autowired
    private ApplicationContext context;
    @Autowired
    private ProcessContainerService processContainerService;
    @Autowired
    private DataProviderService providerService;
    @Autowired
    private DeviceParamRepository deviceParamRepository;
    
    public void create(String operation, TaskObject taskObject) {
        switch (operation) {
            case "CHECKDEVICE": {
                String deviceName = taskObject.getDeviceNameList().get(0);
                DeviceParam deviceParam = deviceParamRepository.findByDeviceNameAndParamName(deviceName, "CHECK_PATH");
                if (deviceParam != null) {
                    taskObject.setTaskId(deviceParam.getDeviceId());
                }

                CheckDevice contextBean = context.getBean(CheckDevice.class);
                contextBean.setTaskObject(taskObject);
                processContainerService.addProcess(contextBean.start(), operation, taskObject.getTaskId());
                break;
            }
            case "DOWNLOAD": {
                providerService.download(taskObject);
                break;
            }
            case "CHECK": {
                CheckTask contextBean = context.getBean(CheckTask.class);
                contextBean.setTaskObject(taskObject);
                processContainerService.addProcess(contextBean.start(), operation, taskObject.getTaskId());
                break;
            }
            case "DEPLOY": {
                Deploy contextBean = context.getBean(Deploy.class);
                contextBean.setTaskObject(taskObject);
                processContainerService.addProcess(contextBean.start(), operation, taskObject.getTaskId());
                break;
            }
            case "RUN": {
                Run contextBean = context.getBean(Run.class);
                contextBean.setTaskObject(taskObject);
                processContainerService.addProcess(contextBean.start(), operation, taskObject.getTaskId());
                break;
            }
            case "STOP": {
                Stop contextBean = context.getBean(Stop.class);
                contextBean.setTaskObject(taskObject);
                processContainerService.addProcess(contextBean.start(), operation, taskObject.getTaskId());
                break;
            }
            case "COLLECT": {
                Collect contextBean = context.getBean(Collect.class);
                contextBean.setTaskObject(taskObject);
                processContainerService.addProcess(contextBean.start(), operation, taskObject.getTaskId());
                break;
            }
            case "UPLOAD": {
                providerService.upload(taskObject);
                break;
            }
            case "PROGRESSINFO": {
                ProgressInfo contextBean = context.getBean(ProgressInfo.class);
                contextBean.setTaskObject(taskObject);
                processContainerService.addProcess(contextBean.start(), operation, taskObject.getTaskId());
                break;
            }
            case "FINALPROGRESSINFO": {
                FinalProgressInfo contextBean = context.getBean(FinalProgressInfo.class);
                contextBean.setTaskObject(taskObject);
                processContainerService.addProcess(contextBean.start(), operation, taskObject.getTaskId());
                break;
            }
        }

    }
}
