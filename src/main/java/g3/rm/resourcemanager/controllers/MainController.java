package g3.rm.resourcemanager.controllers;

import g3.rm.resourcemanager.entities.TaskProcess;
import g3.rm.resourcemanager.repositories.TaskProcessRepository;
import g3.rm.resourcemanager.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import g3.rm.resourcemanager.dtos.TaskObject;

@Controller
public class MainController {
    @Autowired
    private TaskProcessRepository taskProcessRepository;
    @Autowired
    private ProcessCreatorService processCreatorService;
    @Autowired
    private TimerCreatorService timerCreatorService;
    @Autowired
    private LoggerService loggerService;
    @Autowired
    private UpdateParametersService updateParametersService;
    @Autowired
    private FtpTransferService ftpTransferService;

    @GetMapping("/test")
    public @ResponseBody String test() {
        return "Test: OK";
    }

    @GetMapping(value = "/update_params")
    public @ResponseBody String updateParams() {
        return updateParametersService.updateManagerParams();
    }

    @GetMapping(value = "/update_device_params")
    public @ResponseBody String updateDeviceParams() {
        return updateParametersService.updateDeviceParams();
    }

    @GetMapping(value = "/update_task_params")
    public @ResponseBody String updateTaskParams() {
        return updateParametersService.updateProgramParams();
    }

    @PostMapping("/check_device")
    public @ResponseBody String check(@RequestBody TaskObject taskObject) {
        processCreatorService.create("CHECKDEVICE", taskObject);
        return "Check process: Started";
    }

    @GetMapping("/progress_info")
    public @ResponseBody String progressInfo(@RequestParam Long event_id, @RequestParam Long task_id, @RequestParam Long session_id, @RequestParam Integer program_id) {
        TaskObject taskObject = new TaskObject();
        taskObject.setEventId(event_id);
        taskObject.setTaskId(task_id);
        taskObject.setSessionId(session_id);
        taskObject.setProgramId(program_id);
        processCreatorService.create("PROGRESSINFO", taskObject);
        return "Get progress info process: Started";
    }

    @PostMapping("/start")
    public @ResponseBody String start(@RequestBody TaskObject taskObject) {
        processCreatorService.create("DOWNLOAD", taskObject);
        return "Start first stage: Done";
    }

    @GetMapping("/run")
    public @ResponseBody String run(@RequestParam String operation, @RequestParam Long event_id, @RequestParam Integer program_id, @RequestParam Long task_id,  @RequestParam Long session_id) {
        TaskObject taskObject = new TaskObject();
        taskObject.setEventId(event_id);
        taskObject.setTaskId(task_id);
        taskObject.setSessionId(session_id);
        taskObject.setProgramId(program_id);
        processCreatorService.create(operation, taskObject);
        return "Run process [" + operation + "]: Done";
    }

    @GetMapping(value = "/task_log", produces = "text/plain")
    public @ResponseBody String taskLog(@RequestParam Long task_id, @RequestParam Long session_id, @RequestParam Integer page_num, @RequestParam Integer page_size) {
        return loggerService.getPage(task_id, session_id, page_num, page_size);
    }

    @GetMapping(value = "/ftp_upload_log", produces = "text/plain")
    public @ResponseBody String taskLog(@RequestParam Long task_id, @RequestParam Long session_id) {
        return ftpTransferService.uploadExecutionLog(task_id, session_id);
    }

    @GetMapping("/restore")
    public @ResponseBody String restore() {
        timerCreatorService.createRestoreTimer();
        return "Restore process: Done";
    }

    @GetMapping("/list")
    public @ResponseBody Iterable<TaskProcess> list() {
        return taskProcessRepository.findAll();
    }
}
