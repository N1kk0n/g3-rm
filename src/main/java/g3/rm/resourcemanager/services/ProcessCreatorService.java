package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.dtos.Task;
import g3.rm.resourcemanager.entities.DeviceParam;
import g3.rm.resourcemanager.processes.*;
import g3.rm.resourcemanager.repositories.DeviceParamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

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
    
    public void create(String operation, Task task) {
        switch (operation) {
            case "CHECKDEVICE" -> {
                String deviceName = task.getDeviceNameList().get(0);
                DeviceParam deviceParam = deviceParamRepository.findByDeviceNameAndParamName(deviceName, "CHECK_PATH");
                if (deviceParam != null) {
                    task.setTaskId(deviceParam.getDeviceId());
                }

                CheckDevice contextBean = context.getBean(CheckDevice.class);
                contextBean.setTaskObject(task);
                processContainerService.addProcess(contextBean.start(), operation, task.getTaskId());
            }
            case "DOWNLOAD" -> {
                providerService.download(task);
            }
            case "CHECK" -> {
                CheckTask contextBean = context.getBean(CheckTask.class);
                contextBean.setTaskObject(task);
                processContainerService.addProcess(contextBean.start(), operation, task.getTaskId());
            }
            case "DEPLOY" -> {
                Deploy contextBean = context.getBean(Deploy.class);
                contextBean.setTaskObject(task);
                processContainerService.addProcess(contextBean.start(), operation, task.getTaskId());
            }
            case "RUN" -> {
                Run contextBean = context.getBean(Run.class);
                contextBean.setTaskObject(task);
                processContainerService.addProcess(contextBean.start(), operation, task.getTaskId());
            }
            case "STOP" -> {
                Stop contextBean = context.getBean(Stop.class);
                contextBean.setTaskObject(task);
                processContainerService.addProcess(contextBean.start(), operation, task.getTaskId());
            }
            case "COLLECT" -> {
                Collect contextBean = context.getBean(Collect.class);
                contextBean.setTaskObject(task);
                processContainerService.addProcess(contextBean.start(), operation, task.getTaskId());
            }
            case "UPLOAD" -> {
                providerService.upload(task);
            }
            case "PROGRESSINFO" -> {
                ProgressInfo contextBean = context.getBean(ProgressInfo.class);
                contextBean.setTaskObject(task);
                processContainerService.addProcess(contextBean.start(), operation, task.getTaskId());
            }
            case "FINALPROGRESSINFO" -> {
                FinalProgressInfo contextBean = context.getBean(FinalProgressInfo.class);
                contextBean.setTaskObject(task);
                processContainerService.addProcess(contextBean.start(), operation, task.getTaskId());
            }
        }
    }
}
