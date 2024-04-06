package g3.agent.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import g3.agent.jpa_domain.AgentParam;
import g3.agent.jpa_domain.LogicalDeviceParam;
import g3.agent.jpa_domain.TaskParam;
import g3.agent.repositories.AgentParamRepository;
import g3.agent.repositories.DeviceParamRepository;
import g3.agent.repositories.TaskParamRepository;

import java.io.*;
import java.util.List;

@Service
public class FileSystemService {
    @Autowired
    private AgentParamRepository agentParamRepository;
    @Autowired
    private DeviceParamRepository deviceParamRepository;
    @Autowired
    private TaskParamRepository taskParamRepository;

    private final Logger LOGGER = LogManager.getLogger("FileSystemService");

    public FileSystemService() {

    }

    public boolean taskFolderPrepared(long taskId, int programId, List<String> deviceList) {
        try {
            TaskParam taskParam = taskParamRepository.findByProgramIdAndParamName(programId, "HOME");
            if (taskParam == null) {
                LOGGER.error("Task parameter HOME for program with ID: " + programId + " not found");
                return false;
            }
            String homeDirPath = taskParam.getParamValue();
            String taskDirPath = homeDirPath + File.separator + taskId;
            File taskDir = new File(taskDirPath);
            if (!taskDir.exists()) {
                taskDir.mkdir();
            }

            File modFile = new File(taskDirPath + File.separator + taskId + ".mod");
            modFile.createNewFile();
            try (FileOutputStream fileOutputStream = new FileOutputStream(modFile);
                 PrintWriter printWriter = new PrintWriter(fileOutputStream)) {
                for (String deviceName : deviceList) {
                    printWriter.println(deviceName);
                }
            }

            if (templateMarkerExists(programId)) {
                String scriptsDirPath = taskDirPath + "_scripts";
                File scriptsDir = new File(scriptsDirPath);
                if (!scriptsDir.exists()) {
                    scriptsDir.mkdir();
                }
                return bashScriptsPrepared(homeDirPath, taskId, programId, deviceList);
            }
        } catch (IOException ex) {
            LOGGER.error("Error while prepare task folder. Message: " + ex.getMessage(), ex);
            return false;
        }
        return true;
    }

    private boolean bashScriptsPrepared(String homePath, long taskId, int programId, List<String> deviceList) {
        try {
            String deviceName = deviceList.get(0);

            File homeFolder = new File(homePath);
            String folderName = homeFolder.getName();
            String binName = "t_" + folderName.substring(1, folderName.lastIndexOf("."));

            LogicalDeviceParam globalTemplateDirectoryParam = deviceParamRepository.findByDeviceNameAndParamName(deviceName, "GLOBAL_TEMPLATES_DIRECTORY");
            if (globalTemplateDirectoryParam == null) {
                LOGGER.error("Logical device parameter GLOBAL_TEMPLATES_DIRECTORY for device with name: " + deviceName + " not found");
                return false;
            }
            String globalTemplateDirectoryPath = globalTemplateDirectoryParam.getParamValue();

            TaskParam globalTemplateNameParam = taskParamRepository.findByProgramIdAndParamName(programId, "GLOBAL_TEMPLATE_NAME");
            if (globalTemplateNameParam == null) {
                LOGGER.error("Task parameter GLOBAL_TEMPLATE_NAME for program with ID: " + programId + " not found");
                return false;
            }
            String globalTemplateName = globalTemplateNameParam.getParamValue();

            LogicalDeviceParam localTemplateDirectoryParam = deviceParamRepository.findByDeviceNameAndParamName(deviceName, "LOCAL_TEMPLATES_DIRECTORY");
            if (localTemplateDirectoryParam == null) {
                LOGGER.error("Logical device parameter LOCAL_TEMPLATES_DIRECTORY for device with name: " + deviceName + " not found");
                return false;
            }
            String localTemplateDirectoryPath = localTemplateDirectoryParam.getParamValue();

            TaskParam localTemplateNameParam = taskParamRepository.findByProgramIdAndParamName(programId, "LOCAL_TEMPLATE_NAME");
            if (localTemplateNameParam == null) {
                LOGGER.error("Task parameter LOCAL_TEMPLATE_NAME for program with ID: " + programId + " not found");
                return false;
            }
            String localTemplateName = localTemplateNameParam.getParamValue();

            String globalScriptsPath = globalTemplateDirectoryPath + File.separator + globalTemplateName;
            String localScriptsPath = localTemplateDirectoryPath + File.separator + localTemplateName;

            File globalScriptsFolder = new File(globalScriptsPath);
            File localScriptsFolder = new File(localScriptsPath);

            File[] globalScripts = globalScriptsFolder.listFiles();
            if (globalScripts != null) {
                for (File script : globalScripts) {
                    File taskScript = new File(homePath + File.separator +
                                                taskId + "_scripts" + File.separator +
                                                script.getName());
                    taskScript.createNewFile();
                    taskScript.setExecutable(true);

                    try (FileReader fileReader = new FileReader(script);
                         FileWriter fileWriter = new FileWriter(taskScript);
                         BufferedReader bufferedReader = new BufferedReader(fileReader)) {
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            line = line.replaceAll("BIN_NAME", binName);
                            line = line.replaceAll("PROG_DIR", homePath);
                            line = line.replaceAll("TASK_DIR", homePath + File.separator + taskId);
                            fileWriter.write(line + "\n");
                        }
                    }
                }
            }
            File[] localScripts = localScriptsFolder.listFiles();
            if (localScripts != null) {
                for (File script : localScripts) {
                    FileSystemUtils.copyRecursively(script,
                            new File(homePath + File.separator +
                                    taskId + "_scripts" + File.separator +
                                    script.getName()));
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error while prepare scripts. Message: " + ex.getMessage(), ex);
            return false;
        }
        return true;
    }

    public boolean templateMarkerExists(int programId) {
        TaskParam templateMarkerParam = taskParamRepository.findByProgramIdAndParamName(programId, "TEMPLATE_TASK_MARKER");
        if (templateMarkerParam == null) {
            LOGGER.error("Task parameter TEMPLATE_TASK_MARKER for program with ID: " + programId + " not found");
            return false;
        }
        String templateMarker = templateMarkerParam.getParamValue().toLowerCase();
        if (templateMarker.equals("true") || templateMarker.equals("1")) {
            return true;
        } else {
            return false;
        }
    }

    public boolean stopFlagCreated(long taskId, long sessionId, int programId) {
        TaskParam taskParam = taskParamRepository.findByProgramIdAndParamName(programId, "HOME");
        if (taskParam == null) {
            LOGGER.error("Task parameter HOME for program with ID: " + programId + " not found");
            return false;
        }
        String homeDir = taskParam.getParamValue();
        String stopFlagPath = homeDir + File.separator + taskId + "_" + sessionId + ".stopFlag";
        File stopFlag = new File(stopFlagPath);
        try {
            stopFlag.createNewFile();
        } catch (IOException ex) {
            LOGGER.error("Error while creating STOP flag: " + ex.getMessage(), ex);
            return false;
        }
        return true;
    }

    public boolean stopFlagExists(long taskId, long sessionId, int programId) {
        TaskParam taskParam = taskParamRepository.findByProgramIdAndParamName(programId, "HOME");
        if (taskParam == null) {
            LOGGER.error("Task parameter HOME for program with ID: " + programId + " not found");
            return false;
        }
        String homeDir = taskParam.getParamValue();
        String stopFlagPath = homeDir + File.separator + taskId + "_" + sessionId + ".stopFlag";
        File stopFlag = new File(stopFlagPath);
        if (stopFlag.exists()) {
            LOGGER.info("Stop flag: " + stopFlagPath + " exists. Delete...");
            stopFlag.delete();
            return true;
        }
        LOGGER.info("Stop flag: " + stopFlagPath + " not found...");
        return false;
    }

    public void removeTaskFolder(long taskId, int programId) {
        try {
            TaskParam taskParam = taskParamRepository.findByProgramIdAndParamName(programId, "HOME");
            if (taskParam == null) {
                LOGGER.error("Task parameter HOME for program with ID: " + programId + " not found");
                return;
            }
            String homeDir = taskParam.getParamValue();
            String taskDir = homeDir + File.separator + taskId;
            File taskFolder = new File(taskDir);
            if (taskFolder.exists()) {
                if (debugMode()) {
                    LOGGER.info("Deleting [DEBUG MODE] (no actions): " + taskFolder.getAbsolutePath());
                } else {
                    LOGGER.info("Deleting: " + taskFolder.getAbsolutePath());
                    delete(taskFolder.getAbsolutePath());
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error while clear data: " + ex.getMessage(), ex);
        }
    }

    public void removeScriptsFolder(long taskId, int programId) {
        try {
            TaskParam taskParam = taskParamRepository.findByProgramIdAndParamName(programId, "HOME");
            if (taskParam == null) {
                LOGGER.error("Task parameter HOME for program with ID: " + programId + " not found");
                return;
            }
            String homeDir = taskParam.getParamValue();
            String taskDir = homeDir + File.separator + taskId;
            File scriptsFolder = new File(taskDir + "_scripts");
            if (scriptsFolder.exists()) {
                if (debugMode()) {
                    LOGGER.info("Deleting [DEBUG MODE] (no actions): " + scriptsFolder.getAbsolutePath());
                } else {
                    LOGGER.info("Deleting: " + scriptsFolder.getAbsolutePath());
                    delete(scriptsFolder.getAbsolutePath());
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error while clear data: " + ex.getMessage(), ex);
        }
    }

    private void delete(String path) {
        File file = new File(path);
        File[] content = file.listFiles();
        if (content != null) {
            for (File contentItem : content) {
                delete(contentItem.getAbsolutePath());
            }
        }
        if(!file.delete()) LOGGER.error("Error while delete file: " + file.getAbsolutePath());
    }

    private boolean debugMode() {
        AgentParam debugModeParam = agentParamRepository.getByName("AGENT_DEBUG_MODE");
        if (debugModeParam == null) {
            LOGGER.error("Agent parameter with name: AGENT_DEBUG_MODE not found");
            return false;
        }
        String debugMode = debugModeParam.getValue();
        debugMode = debugMode.toLowerCase();
        if (debugMode.equals("1") || debugMode.equals("true")) {
            return true;
        }
        return false;
    }
}
