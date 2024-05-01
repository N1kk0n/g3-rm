package g3.rm.resourcemanager.services;

import g3.rm.resourcemanager.jpa_domain.AgentParam;
import g3.rm.resourcemanager.jpa_domain.LogicalDeviceParam;
import g3.rm.resourcemanager.repositories.AgentParamRepository;
import g3.rm.resourcemanager.repositories.DeviceParamRepository;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Properties;

@Service
public class MailSenderService {
    @Autowired
    private AgentParamRepository agentParamRepository;
    @Autowired
    private DeviceParamRepository deviceParamRepository;

    private final Logger LOGGER = LogManager.getLogger("MailSenderService");

    public void sendDeviceBrokenMail(String deviceName, int code) {
        if (debugMode()) {
            LOGGER.debug("Alarm mail is turned off for device [DEBUG MODE]: " + deviceName);
            return;
        }
        if (mailAlarmDisabled(deviceName)) {
            LOGGER.info("Alarm mail is disabled for device: " + deviceName);
            return;
        }
        AgentParam mailHostParam = agentParamRepository.getByName("ALARM_MAIL_HOST");
        if (mailHostParam == null) {
            LOGGER.error("Agent parameter ALARM_MAIL_HOST not found");
            return;
        }

        AgentParam mailHostAddress = agentParamRepository.getByName("ALARM_MAIL_ADDRESS");
        if (mailHostAddress == null) {
            LOGGER.error("Agent parameter ALARM_MAIL_ADDRESS not found");
            return;
        }
        String mailHost = mailHostParam.getValue();
        String mailAddress = mailHostAddress.getValue();
        String subject = "Gambit check device: " + deviceName;
        String text = "======================\n Error on check device: " + deviceName + ". Check device ended with code: " + code + "\n======================";
        sendEmailMessage(mailAddress, mailHost, subject, text);
    }

    private void sendEmailMessage(String receiverMail, String mailServerHost, String subject, String content) {
        Properties props = new Properties();
        props.put("mail.smtp.host", mailServerHost);
        Session session = Session.getInstance(props);

        try {
            Message msg = new MimeMessage(session);
            InternetAddress[] address = new InternetAddress[]{new InternetAddress(receiverMail)};
            msg.setRecipients(Message.RecipientType.TO, address);
            msg.setSubject(subject);
            msg.setSentDate(new Date());
            msg.setText(content);
            Transport.send(msg);
        } catch (Exception ex) {
            LOGGER.error("Error while send email message: " + ex.getMessage());
        }
    }

    private boolean mailAlarmDisabled(String deviceName) {
        LogicalDeviceParam mailAlarmParam = deviceParamRepository.findByDeviceNameAndParamName(deviceName, "MAIL_ALARM");
        if (mailAlarmParam == null) {
            LOGGER.error("Logical device parameter MAIL_ALARM for device with name: " + deviceName + " not found. Mail alarm enabled");
            return false;
        }
        String mailAlarmDisabled = mailAlarmParam.getParamValue();
        mailAlarmDisabled = mailAlarmDisabled.toLowerCase();
        if (mailAlarmDisabled.equals("0") || mailAlarmDisabled.equals("false")) {
            return false;
        }
        return true;
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
