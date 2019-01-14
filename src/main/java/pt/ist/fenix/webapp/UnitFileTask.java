package pt.ist.fenix.webapp;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcessState;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.idcards.domain.SantanderPhotoEntry;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;
import org.slf4j.Logger;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import pt.ist.fenixedu.contracts.domain.accessControl.ActiveEmployees;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveGrantOwner;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveResearchers;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonContractSituation;
import pt.ist.fenixedu.contracts.domain.util.CategoryType;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;
import pt.sibscartoes.portal.wcf.IRegistersInfo;
import pt.sibscartoes.portal.wcf.dto.FormData;
import pt.sibscartoes.portal.wcf.dto.RegisterData;
import pt.sibscartoes.portal.wcf.tui.ITUIDetailService;
import pt.sibscartoes.portal.wcf.tui.dto.TUIResponseData;
import pt.sibscartoes.portal.wcf.tui.dto.TuiPhotoRegisterData;
import pt.sibscartoes.portal.wcf.tui.dto.TuiSignatureRegisterData;

public class UnitFileTask extends CustomTask {

    protected final Logger logger = getLogger();

    final private String USERNAME = "USERNAME";
    final private String PASSWORD = "PASSWORD";

    @Override
    public TxMode getTxMode() {

        return TxMode.READ;

    }
    
    private static String alamedaAddr = "Avenida Rovisco Pais, 1";
    private static String alamedaZip = "1049-001";
    private static String alamedaTown = "Lisboa";
    private static String tagusAddr = "Av. Prof. Doutor Aníbal Cavaco Silva";
    private static String tagusZip = "2744-016";
    private static String tagusTown = "Porto Salvo";
    private static String itnAddr = "Estrada Nacional 10 (ao Km 139,7)";
    private static String itnZip = "2695-066";
    private static String itnTown = "Bobadela";
    private static String IST_FULL_NAME = "Instituto Superior Técnico";

    @Override
    public void runTask() throws IOException {
        createRegister(); //New webservice
        getRegister(); //Old webservice used for testing
    }

    private void getRegister() {
        
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();

        factory.setServiceClass(IRegistersInfo.class);
        factory.setAddress("https://portal.sibscartoes.pt/wcf/RegistersInfo.svc");
        factory.setBindingId("http://schemas.xmlsoap.org/wsdl/soap12/");
        factory.getFeatures().add(new WSAddressingFeature());
        
        //Add loggers
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.getOutInterceptors().add(new LoggingOutInterceptor());

        IRegistersInfo port = (IRegistersInfo) factory.create();

        /*define WSDL policy*/
        Client client = ClientProxy.getClient(port);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        
        //Add username and password properties
        http.getAuthorization().setUserName(USERNAME);
        http.getAuthorization().setPassword(PASSWORD);

        Person person = FenixFramework.getDomainObject("2306397441297");
        final String userName = Strings.padEnd(person.getUsername(), 9, 'x');
        //
        RegisterData statusInformation = port.getRegister(userName);

        String result = userName + " : " + statusInformation.getStatusDate().getValue().replaceAll("-", "/") + " : "
                + statusInformation.getStatus().getValue() + " - " + statusInformation.getStatusDesc().getValue();

        taskLog("getRegister => %s%n", result);
        
        FormData formData = port.getFormStatus(userName);
        
        result = formData.getEntityCode().getValue() + " - " + formData.getIdentRegNum().getValue() + " - "
                + formData.getNDoc().getValue() + formData.getStatus().getValue() + " - " + formData.getStatusDate().getValue();
        
        String template = "%s | Entity: %s | IdentRegNum: %s | NDoc: %s | Status: %s | Date: %s";
        result = String.format(template, userName, formData.getEntityCode().getValue(), formData.getIdentRegNum().getValue(),
                formData.getNDoc().getValue(), formData.getStatus().getValue(), formData.getIdentRegNum().getValue());

        taskLog("getFormData => %s%n", result);
    }

    private void createRegister() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();

        factory.setServiceClass(ITUIDetailService.class);
        factory.setAddress("https://portal.sibscartoes.pt/tstwcfv2/services/TUIDetailService.svc");
        factory.setBindingId("http://schemas.xmlsoap.org/wsdl/soap12/");
        factory.getFeatures().add(new WSAddressingFeature());

        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.getOutInterceptors().add(new LoggingOutInterceptor());

        ITUIDetailService port = (ITUIDetailService) factory.create();
        
        /*define WSDL policy*/
        Client client = ClientProxy.getClient(port);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        //Add username and password properties
        http.getAuthorization().setUserName(USERNAME);
        http.getAuthorization().setPassword(PASSWORD);

        Person me = FenixFramework.getDomainObject("283781374150114");
        Person person = FenixFramework.getDomainObject("2306397441297");

        ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();

        String tuiEntry = generateLine(person, executionYear);
        TuiPhotoRegisterData photo = getOrCreateSantanderPhoto(me);
        TuiSignatureRegisterData signature = new TuiSignatureRegisterData();

        taskLog("line: %s %d%n", tuiEntry, tuiEntry.length());
        TUIResponseData tuiResponse = port.saveRegister(tuiEntry, photo, signature);
        taskLog("Response Status: %s  -- Description: %s%n", tuiResponse.getStatus().getValue(),
                tuiResponse.getStatusDescription().getValue());
        taskLog("Response Line: %s%n", tuiResponse.getTuiResponseLine().getValue());

    }

    private TuiPhotoRegisterData getOrCreateSantanderPhoto(Person person) {
        final QName FILE_NAME =
                new QName("http://schemas.datacontract.org/2004/07/SibsCards.Wcf.Services.DataContracts", "FileName");
        final QName FILE_EXTENSION =
                new QName("http://schemas.datacontract.org/2004/07/SibsCards.Wcf.Services.DataContracts", "Extension");
        final QName FILE_CONTENTS =
                new QName("http://schemas.datacontract.org/2004/07/SibsCards.Wcf.Services.DataContracts", "FileContents");
        final QName FILE_SIZE =
                new QName("http://schemas.datacontract.org/2004/07/SibsCards.Wcf.Services.DataContracts", "Size");

        final String EXTENSION = ".jpeg";

        TuiPhotoRegisterData photo = new TuiPhotoRegisterData();

        SantanderPhotoEntry photoEntry = SantanderPhotoEntry.getOrCreatePhotoEntryForPerson(person);
        byte[] photo_contents = photoEntry.getPhotoAsByteArray();

        photo.setFileContents(new JAXBElement<byte[]>(FILE_CONTENTS, byte[].class, photo_contents));
        photo.setSize(new JAXBElement<String>(FILE_SIZE, String.class,
                new Integer(photo_contents.length).toString()));
        photo.setExtension(new JAXBElement<String>(FILE_EXTENSION, String.class, new String(".jpeg")));
        photo.setFileName(new JAXBElement<String>(FILE_NAME, String.class, "foto")); //TODO

        return photo;

    }

    private String generateLine(Person person, ExecutionYear executionYear) {
        /*
         * 1. Teacher
         * 2. Researcher
         * 3. Employee
         * 4. GrantOwner
         * 5. Student
         */
        String line = "";
        if (treatAsStudent(person, executionYear)) {
            line = createLine(person, "STUDENT", executionYear);
        } else if (treatAsTeacher(person)) {
            line = createLine(person, "TEACHER", executionYear);
        } else if (treatAsResearcher(person)) {
            line = createLine(person, "RESEARCHER", executionYear);
        } else if (treatAsEmployee(person)) {
            line = createLine(person, "EMPLOYEE", executionYear);
        } else if (treatAsGrantOwner(person)) {
            line = createLine(person, "GRANT_OWNER", executionYear);
        }

        return line;
    }

    private boolean treatAsTeacher(Person person) {
        if (person.getTeacher() != null) {
            return person.getTeacher().isActiveContractedTeacher();
        }
        return false;
    }

    private boolean treatAsResearcher(Person person) {
        if (person.getEmployee() != null) {
            return new ActiveResearchers().isMember(person.getUser());
        }
        return false;
    }

    private boolean treatAsEmployee(Person person) {
        if (person.getEmployee() != null) {
            return person.getEmployee().isActive();
        }
        return false;
    }

    private boolean treatAsGrantOwner(Person person) {
        return (isGrantOwner(person)) || (new ActiveGrantOwner().isMember(person.getUser()) && person.getEmployee() != null
                && !new ActiveEmployees().isMember(person.getUser()) && person.getPersonProfessionalData() != null);
    }

    private boolean treatAsStudent(Person person, ExecutionYear executionYear) {
        if (person.getStudent() != null) {
            final List<Registration> activeRegistrations = person.getStudent().getActiveRegistrations();
            for (final Registration registration : activeRegistrations) {
                if (registration.isBolonha() && !registration.getDegreeType().isEmpty()) {
                    return true;
                }
            }
            final InsuranceEvent event = person.getInsuranceEventFor(executionYear);
            final PhdIndividualProgramProcess phdIndividualProgramProcess =
                    event != null && event.isClosed() ? find(person.getPhdIndividualProgramProcessesSet()) : null;
            return (phdIndividualProgramProcess != null);
        }
        return false;
    }

    private boolean isGrantOwner(final Person person) {
        if (new ActiveGrantOwner().isMember(person.getUser())) {
            final PersonContractSituation currentGrantOwnerContractSituation = person.getPersonProfessionalData() != null ? person
                    .getPersonProfessionalData().getCurrentPersonContractSituationByCategoryType(CategoryType.GRANT_OWNER) : null;
            if (currentGrantOwnerContractSituation != null && currentGrantOwnerContractSituation.getProfessionalCategory() != null
                    && person.getEmployee() != null && person.getEmployee().getCurrentWorkingPlace() != null) {
                return true;
            }
        }
        return false;
    }

    private PhdIndividualProgramProcess find(final Set<PhdIndividualProgramProcess> phdIndividualProgramProcesses) {
        PhdIndividualProgramProcess result = null;
        for (final PhdIndividualProgramProcess process : phdIndividualProgramProcesses) {
            if (process.getActiveState() == PhdIndividualProgramProcessState.WORK_DEVELOPMENT) {
                if (result != null) {
                    return null;
                }
                result = process;
            }
        }
        return result;
    }

    public String createLine(Person person, String role, ExecutionYear executionYear) {

        StringBuilder strBuilder = new StringBuilder(1505);

        String recordType = "2";

        String idNumber = makeStringBlock(person.getUsername(), 10);

        String[] names = harvestNames(person.getName());
        String name = makeStringBlock(names[0], 15);
        String surname = makeStringBlock(names[1], 15);
        String middleNames = makeStringBlock(names[2], 40);

        String degreeCode = makeStringBlock(getDegreeDescription(person, role, executionYear), 16);
        if (role.equals("STUDENT") && degreeCode.startsWith(" ")) {
            return null;
        }

        CampusAddress campusAddr = getCampusAddress(person, role);
        if (campusAddr == null) {
            return null;
        }
        String address1 = makeStringBlock(campusAddr.getAddress(), 50);
        String address2 = makeStringBlock((IST_FULL_NAME + (degreeCode == null ? "" : " " + degreeCode)).trim(), 50);

        String zipCode = campusAddr.getZip();
        String town = makeStringBlock(campusAddr.getTown(), 30);

        String homeCountry = makeStringBlock("", 10);

        String residenceCountry = makeStringBlock(person.getUsername(), 10); // As stipulated this field will carry the istId instead.

        String expireDate = getExpireDate(executionYear);

        String backNumber = makeZeroPaddedNumber(Integer.parseInt(person.getUsername().substring(3)), 10);

        String curricularYear = "00";
        String executionYear_field = "00000000";

        String unit = makeStringBlock("", 30); //Size changed from 11 to 30
        if (role.equals("TEACHER")) {
            unit = makeStringBlock(person.getTeacher().getDepartment().getAcronym(), 30);
        }

        String accessContrl = makeStringBlock("", 10);

        String expireData_AAMM = expireDate.substring(7) + "08"; //TODO size 4

        String templateCode = makeStringBlock("", 10); //TODO

        String actionCode = makeStringBlock("NOVO", 4); //TODO

        String roleCode = getRoleCode(role);

        String roleDesc = makeStringBlock(getRoleDescripriton(role), 20);

        String idDocumentType = makeStringBlock("0", 1); // TODO

        String checkDigit = makeStringBlock("", 1); // TODO

        String cardType = makeStringBlock("00", 2); // TODO

        String expedictionCode = makeStringBlock("00", 2); // TODO

        String detourAdress1 = makeStringBlock("", 50); // TODO

        String detourAdress2 = makeStringBlock("", 50); // TODO

        String detourAdress3 = makeStringBlock("", 50); // TODO

        String detourZipCode = makeStringBlock("", 8); // TODO

        String detourTown = makeStringBlock("", 30); // TODO

        String aditionalData = makeStringBlock("1", 1); // TODO

        //What characters are allowed
        String cardName = makeStringBlock(names[0].toUpperCase() + " " + names[1].toUpperCase(), 40); // TODO

        String email = makeStringBlock("", 100); // TODO

        String phone = makeStringBlock("", 20); // TODO

        String photoFlag = makeStringBlock("0", 1); // TODO

        String photoRef = makeStringBlock("", 32); // TODO

        String signatureFlag = makeStringBlock("0", 1); // TODO

        String signatureRef = makeStringBlock("", 32); // TODO

        String digCertificateFlag = makeStringBlock("0", 1); // TODO

        String digCertificateRef = makeStringBlock("", 32); // TODO

        String filler = makeStringBlock("", 682);

        strBuilder.append(recordType); //0
        strBuilder.append(idNumber); //1
        strBuilder.append(name); //2
        strBuilder.append(surname); //3
        strBuilder.append(middleNames); //4
        strBuilder.append(address1); //5
        strBuilder.append(address2); //6
        strBuilder.append(zipCode); //7
        strBuilder.append(town); //8
        strBuilder.append(homeCountry); //9
        strBuilder.append(residenceCountry); //10
        strBuilder.append(expireDate); //11
        strBuilder.append(degreeCode); //12
        strBuilder.append(backNumber); //13
        strBuilder.append(curricularYear); //14
        strBuilder.append(executionYear_field); //15
        strBuilder.append(unit); //16
        strBuilder.append(accessContrl); //17
        strBuilder.append(expireData_AAMM); //18
        strBuilder.append(templateCode); //19
        strBuilder.append(actionCode); //20
        strBuilder.append(roleCode); //21
        strBuilder.append(roleDesc); //22
        strBuilder.append(idDocumentType); //23
        strBuilder.append(checkDigit); //24
        strBuilder.append(cardType); //25
        strBuilder.append(expedictionCode); //26
        strBuilder.append(detourAdress1); //27
        strBuilder.append(detourAdress2); //28
        strBuilder.append(detourAdress3); //29
        strBuilder.append(detourZipCode); //30
        strBuilder.append(detourTown); //31
        strBuilder.append(aditionalData); //32
        strBuilder.append(cardName); //33
        strBuilder.append(email); //34
        strBuilder.append(phone); //35
        strBuilder.append(photoFlag); //36
        strBuilder.append(photoRef); //37
        strBuilder.append(signatureFlag); //38
        strBuilder.append(signatureRef); //39
        strBuilder.append(digCertificateFlag); //40
        strBuilder.append(digCertificateRef); //41
        strBuilder.append(filler); //42

        taskLog("recordType: %s -- size: %s%n", recordType, recordType.length());
        taskLog("idNumber: %s -- size: %s%n", idNumber, idNumber.length());
        taskLog("name: %s -- size: %s%n", name, name.length());
        taskLog("surname: %s -- size: %s%n", surname, surname.length());
        taskLog("middleNames: %s -- size: %s%n", middleNames, middleNames.length());
        taskLog("address1: %s -- size: %s%n", address1, address1.length());
        taskLog("address2: %s -- size: %s%n", address2, address2.length());
        taskLog("zipCode: %s -- size: %s%n", zipCode, zipCode.length());
        taskLog("town: %s -- size: %s%n", town, town.length());
        taskLog("homeCountry: %s -- size: %s%n", homeCountry, homeCountry.length());
        taskLog("residenceCountry: %s -- size: %s%n", residenceCountry, residenceCountry.length());
        taskLog("expireDate: %s -- size: %s%n", expireDate, expireDate.length());
        taskLog("degreeCode: %s -- size: %s%n", degreeCode, degreeCode.length());
        taskLog("backNumber: %s -- size: %s%n", backNumber, backNumber.length());
        taskLog("curricularYear: %s -- size: %s%n", curricularYear, curricularYear.length());
        taskLog("executionYear_field: %s -- size: %s%n", executionYear_field, executionYear_field.length());
        taskLog("unit: %s -- size: %s%n", unit, unit.length());
        taskLog("accessContrl: %s -- size: %s%n", accessContrl, accessContrl.length());
        taskLog("expireData_AAMM: %s -- size: %s%n", expireData_AAMM, expireData_AAMM.length());
        taskLog("templateCode: %s -- size: %s%n", templateCode, templateCode.length());
        taskLog("actionCode: %s -- size: %s%n", actionCode, actionCode.length());
        taskLog("roleCode: %s -- size: %s%n", roleCode, roleCode.length());
        taskLog("roleDesc: %s -- size: %s%n", roleDesc, roleDesc.length());
        taskLog("idDocumentType: %s -- size: %s%n", idDocumentType, idDocumentType.length());
        taskLog("checkDigit: %s -- size: %s%n", checkDigit, checkDigit.length());
        taskLog("cardType: %s -- size: %s%n", cardType, cardType.length());
        taskLog("expedictionCode: %s -- size: %s%n", expedictionCode, expedictionCode.length());
        taskLog("detourAdress1: %s -- size: %s%n", detourAdress1, detourAdress1.length());
        taskLog("detourAdress2: %s -- size: %s%n", detourAdress2, detourAdress2.length());
        taskLog("detourAdress3: %s -- size: %s%n", detourAdress3, detourAdress3.length());
        taskLog("detourZipCode: %s -- size: %s%n", detourZipCode, detourZipCode.length());
        taskLog("detourTown: %s -- size: %s%n", detourTown, detourTown.length());
        taskLog("aditionalData: %s -- size: %s%n", aditionalData, aditionalData.length());
        taskLog("cardName: %s -- size: %s%n", cardName, cardName.length());
        taskLog("email: %s -- size: %s%n", email, email.length());
        taskLog("phone: %s -- size: %s%n", phone, phone.length());
        taskLog("photoFlag: %s -- size: %s%n", photoFlag, photoFlag.length());
        taskLog("photoRef: %s -- size: %s%n", photoRef, photoRef.length());
        taskLog("signatureFlag: %s -- size: %s%n", signatureFlag, signatureFlag.length());
        taskLog("signatureRef: %s -- size: %s%n", signatureRef, signatureRef.length());
        taskLog("digCertificateFlag: %s -- size: %s%n", digCertificateFlag, digCertificateFlag.length());
        taskLog("digCertificateRef: %s -- size: %s%n", digCertificateRef, digCertificateRef.length());
        taskLog("filler: %s -- size: %s%n", filler, filler.length());

        return strBuilder.toString();
    }

    private String getRoleCode(String role) {
        switch (role) {
        case "STUDENT":
            return "01";

        case "TEACHER":
            return "02";

        case "EMPLOYEE":
            return "03";

        default:
            return "99";
        }
    }

    private String[] harvestNames(String name) {
        String[] result = new String[3];
        String purgedName = purgeString(name); //Remove special characters
        String cleanedName = Strings.nullToEmpty(purgedName).trim();
        String[] names = cleanedName.split(" ");
        result[0] = names[0].length() > 15 ? names[0].substring(0, 15) : names[0];
        result[1] = names[names.length - 1].length() > 15 ? names[names.length - 1].substring(0, 15) : names[names.length - 1];
        String midNames = names.length > 2 ? names[1] : "";
        for (int i = 2; i < (names.length - 1); i++) {
            if (midNames.length() + names[i].length() + 1 > 40) {
                break;
            }
            midNames += " ";
            midNames += names[i];
        }
        result[2] = midNames;
        return result;
    }

    private String purgeString(final String name) {
        if (!CharMatcher.javaLetter().or(CharMatcher.whitespace()).matchesAllOf(name)) {
            final char[] ca = new char[name.length()];
            int j = 0;
            for (int i = 0; i < name.length(); i++) {
                final char c = name.charAt(i);
                if (Character.isLetter(c) || c == ' ') {
                    ca[j++] = c;
                }
            }
            return new String(ca);
        }
        return name;
    }

    private String getDegreeDescription(final Person person, String roleType, ExecutionYear executionYear) {
        if (roleType.equals("STUDENT") || roleType.equals("GRANT_OWNER")) {
            final PhdIndividualProgramProcess process = getPhdProcess(person);
            if (process != null) {
                logger.debug("phdProcess: " + process.getExternalId());
                return process.getPhdProgram().getAcronym();
            }
            final Degree degree = getDegree(person, executionYear);
            if (degree != null) {
                return degree.getSigla();
            }
        }
        if (roleType.equals("TEACHER")) {
            final Teacher teacher = person.getTeacher();
            final Department department = teacher.getDepartment();
            if (department != null) {
                return department.getAcronym();
            }
        }
        return "";
    }

    private PhdIndividualProgramProcess getPhdProcess(final Person person) {
        final InsuranceEvent event = person.getInsuranceEventFor(ExecutionYear.readCurrentExecutionYear());
        return event != null && event.isClosed() ? find(person.getPhdIndividualProgramProcessesSet()) : null;
    }

    private Degree getDegree(Person person, ExecutionYear executionYear) {
        final Student student = person.getStudent();
        if (student == null) {
            return null;
        }

        final DateTime begin = executionYear.getBeginDateYearMonthDay().toDateTimeAtMidnight();
        final DateTime end = executionYear.getEndDateYearMonthDay().toDateTimeAtMidnight();
        final Set<StudentCurricularPlan> studentCurricularPlans = new HashSet<StudentCurricularPlan>();
        StudentCurricularPlan pickedSCP;

        for (final Registration registration : student.getRegistrationsSet()) {
            if (!registration.isActive()) {
                continue;
            }
            if (registration.getDegree().isEmpty()) {
                continue;
            }
            final RegistrationProtocol registrationAgreement = registration.getRegistrationProtocol();
            if (!registrationAgreement.allowsIDCard()) {
                continue;
            }
            final DegreeType degreeType = registration.getDegreeType();
            if (!degreeType.isBolonhaType()) {
                continue;
            }
            for (final StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
                if (studentCurricularPlan.isActive()) {
                    if (degreeType.isBolonhaDegree() || degreeType.isBolonhaMasterDegree()
                            || degreeType.isIntegratedMasterDegree() || degreeType.isAdvancedSpecializationDiploma()) {
                        studentCurricularPlans.add(studentCurricularPlan);
                    } else {
                        final RegistrationState registrationState = registration.getActiveState();
                        if (registrationState != null) {
                            final DateTime dateTime = registrationState.getStateDate();
                            if (!dateTime.isBefore(begin) && !dateTime.isAfter(end)) {
                                studentCurricularPlans.add(studentCurricularPlan);
                            }
                        }
                    }
                }
            }
        }
        if (studentCurricularPlans.isEmpty()) {
            return null;
        }
        pickedSCP = Collections.max(studentCurricularPlans, new Comparator<StudentCurricularPlan>() {

            @Override
            public int compare(final StudentCurricularPlan o1, final StudentCurricularPlan o2) {
                final DegreeType degreeType1 = o1.getDegreeType();
                final DegreeType degreeType2 = o2.getDegreeType();
                if (degreeType1 == degreeType2) {
                    final YearMonthDay yearMonthDay1 = o1.getStartDateYearMonthDay();
                    final YearMonthDay yearMonthDay2 = o2.getStartDateYearMonthDay();
                    final int c = yearMonthDay1.compareTo(yearMonthDay2);
                    return c == 0 ? o1.getExternalId().compareTo(o2.getExternalId()) : c;
                } else {
                    return degreeType1.compareTo(degreeType2);
                }
            }

        });
        return pickedSCP.getRegistration().getDegree();
    }

    private CampusAddress getCampusAddress(Person person, String role) {
        Space campus = null;
        Map<String, CampusAddress> campi = getCampi();
        switch (role) {
        case "STUDENT":
            boolean matched = false;
            if (person.getStudent() != null) {
                final List<Registration> activeRegistrations = person.getStudent().getActiveRegistrations();
                for (final Registration registration : activeRegistrations) {
                    if (registration.isBolonha() && !registration.getDegreeType().isEmpty()) {
                        matched = true;
                        campus = person.getStudent().getLastActiveRegistration().getCampus();
                    }
                }
            }
            if (!matched) {
                campus = FenixFramework.getDomainObject("2448131360897");
            }
            break;
        case "EMPLOYEE":
            try {
                campus = person.getEmployee().getCurrentCampus();
            } catch (NullPointerException npe) {
                return null;
            }
            break;
        case "TEACHER":
            try {
                campus = person.getPersonProfessionalData().getGiafProfessionalDataByCategoryType(CategoryType.TEACHER)
                        .getCampus();
            } catch (NullPointerException npe) {
                return null;
            }
            break;
        case "RESEARCHER":
            try {
                campus = person.getPersonProfessionalData().getGiafProfessionalDataByCategoryType(CategoryType.RESEARCHER)
                        .getCampus();
            } catch (NullPointerException npe) {
                return null;
            }
            break;
        case "GRANT_OWNER":
            try {
                campus = person.getPersonProfessionalData().getGiafProfessionalDataByCategoryType(CategoryType.GRANT_OWNER)
                        .getCampus();
            } catch (NullPointerException npe) {
                return null;
            }
            break;
        default:
            break;
        }
        if (campus == null) {
            return null;
        }

        if (campus.getName().equals("Alameda")) {
            return campi.get("alameda");
        }

        if (campus.getName().equals("Taguspark")) {
            return campi.get("tagus");
        }

        if (campus.getName().equals("Tecnológico e Nuclear")) {
            return campi.get("itn");
        }

        return null;

    }

    private String getRoleDescripriton(String role) {
        switch (role) {
        case "STUDENT":
            return "Estudante/Student";
        case "TEACHER":
            return "Docente/Faculty";
        case "EMPLOYEE":
            return "Funcionario/Staff";
        case "RESEARCHER":
            return "Invest./Researcher";
        case "GRANT_OWNER":
            return "Bolseiro/Grant Owner";
        default:
            return "00";
        }
    }

    private String getExpireDate(ExecutionYear year) {
        String result = "";
        int beginYear = year.getBeginCivilYear();
        int endYear = beginYear + 3;
        result = beginYear + "/" + endYear;
        return result;
    }

    private String makeZeroPaddedNumber(int number, int size) {
        if (String.valueOf(number).length() > size) {
            throw new DomainException("Number has more digits than allocated room.");
        }
        String format = "%0" + size + "d";
        return String.format(format, number);
    }

    private String makeStringBlock(String content, int size) {
        int fillerLength = size - content.length();
        if (fillerLength < 0) {
            throw new DomainException("Content is bigger than string block.");
        }
        StringBuilder blockBuilder = new StringBuilder(size);
        blockBuilder.append(content);

        for (int i = 0; i < fillerLength; i++) {
            blockBuilder.append(" ");
        }

        return blockBuilder.toString();
    }

    private class CampusAddress {
        private final String address;
        private final String zip;
        private final String town;

        private CampusAddress(String address, String zip, String town) {
            this.address = address;
            this.zip = zip;
            this.town = town;
        }

        public String getAddress() {
            return address;
        }

        public String getZip() {
            return zip;
        }

        public String getTown() {
            return town;
        }
    }

    private Map<String, CampusAddress> getCampi() {
        Map<String, CampusAddress> exports = new HashMap<String, CampusAddress>();
        exports.put("alameda", new CampusAddress(alamedaAddr, alamedaZip, alamedaTown));
        exports.put("tagus", new CampusAddress(tagusAddr, tagusZip, tagusTown));
        exports.put("itn", new CampusAddress(itnAddr, itnZip, itnTown));
        return exports;
    }

}