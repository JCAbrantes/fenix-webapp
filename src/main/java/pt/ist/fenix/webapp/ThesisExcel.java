package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.degreeStructure.CycleCourseGroup;
import org.fenixedu.academic.domain.person.Gender;
import org.fenixedu.academic.domain.person.IDDocumentType;
import org.fenixedu.academic.domain.student.curriculum.ConclusionProcess;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.studentCurriculum.Substitution;
import org.fenixedu.academic.domain.thesis.Thesis;
import org.fenixedu.academic.domain.thesis.ThesisEvaluationParticipant;
import org.fenixedu.academic.domain.thesis.ThesisParticipationType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class ThesisExcel extends CustomTask {

    public List<String> no_dismissal_no_enrolment = new ArrayList<String>();
    public List<String> dismissal_no_enrolment = new ArrayList<String>();
    public List<String> dismissal_more_enrolments = new ArrayList<String>();
    public List<String> thesis_is_null = new ArrayList<String>();
    public List<String> enrolment_is_null = new ArrayList<String>();

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws IOException {

        Spreadsheet spreasheet = createSheet("thesisReport");
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        spreasheet.exportToXLSSheet(byteArrayOS);

        try {
            FileOutputStream fos = new FileOutputStream(new File("thesisReport.xls"));
            byteArrayOS.writeTo(fos);
            fos.close();
        } catch (IOException ioe) {
        }

        byteArrayOS.close();
    }

    public Spreadsheet createSheet(String docTitle) {
        Spreadsheet spreadsheet = new Spreadsheet(docTitle);

        spreadsheet.setHeader("NomeCompleto"); //0
        spreadsheet.setHeader("Sexo"); //1
        spreadsheet.setHeader("DataNascimento"); //2
        spreadsheet.setHeader("NumIdentificacao"); //3
        spreadsheet.setHeader("TipoIdentificacao"); //4
        spreadsheet.setHeader("OutroTipoIdentificacao"); //5
        spreadsheet.setHeader("PaisNacionalidade"); //6
        spreadsheet.setHeader("OutraNacionalidade"); //Optional
        spreadsheet.setHeader("Morada"); //Optional
        spreadsheet.setHeader("Telefone");//Optional
        spreadsheet.setHeader("Email"); //10
        spreadsheet.setHeader("OutroEmail"); //Optional
        spreadsheet.setHeader("CodEstabelecimentoPortugues"); //12
        spreadsheet.setHeader("codUnidadeOrganica"); //13
        spreadsheet.setHeader("CodCurso"); //14
        spreadsheet.setHeader("Curso"); //15
        spreadsheet.setHeader("CodEspecializacao"); //16
        spreadsheet.setHeader("Especializacao"); //17
        spreadsheet.setHeader("TituloTrabalho"); //18
        spreadsheet.setHeader("NomeOrientador1"); //19
        spreadsheet.setHeader("NumIdentificacao_Orientador1"); //20
        spreadsheet.setHeader("TipoIdentificacao_Orientador1"); //21
        spreadsheet.setHeader("OutroTipoIdentificacao_Orientador1"); //22
        spreadsheet.setHeader("ORCID_Orientador1"); //Optional
        spreadsheet.setHeader("NomeOrientador2"); //Optional
        spreadsheet.setHeader("NumIdentificacao_Orientador2"); //Optional
        spreadsheet.setHeader("TipoIdentificacao_Orientador2"); //Optional
        spreadsheet.setHeader("OutroTipoIdentificacao_Orientador2"); //Optional
        spreadsheet.setHeader("ORCID_Orientador2"); //Optional
        spreadsheet.setHeader("NomeOrientador3"); //Optional
        spreadsheet.setHeader("NumIdentificacao_Orientador3"); //Optional
        spreadsheet.setHeader("TipoIdentificacao_Orientador3"); //Optional
        spreadsheet.setHeader("OutroTipoIdentificacao_Orientador3"); //Optional
        spreadsheet.setHeader("ORCID_Orientador3"); //Optional
        spreadsheet.setHeader("NomeOrientador4"); //Optional
        spreadsheet.setHeader("NumIdentificacao_Orientador4"); //Optional
        spreadsheet.setHeader("TipoIdentificacao_Orientador4"); //Optional
        spreadsheet.setHeader("OutroTipoIdentificacao_Orientador4"); //Optional
        spreadsheet.setHeader("ORCID_Orientador4"); //Optional
        spreadsheet.setHeader("NomeOrientador5"); //Optional
        spreadsheet.setHeader("NumIdentificacao_Orientador5"); //Optional
        spreadsheet.setHeader("TipoIdentificacao_Orientador5"); //Optional
        spreadsheet.setHeader("OutroTipoIdentificacao_Orientador5"); //Optional
        spreadsheet.setHeader("ORCID_Orientador5"); //Optional
        spreadsheet.setHeader("Co_orientador"); //Optional
        spreadsheet.setHeader("Palavras_chave"); //45
        spreadsheet.setHeader("Curso_Parceria"); //46
        spreadsheet.setHeader("Estabelecimento1"); //47
        spreadsheet.setHeader("Estabelecimento2"); //Optional
        spreadsheet.setHeader("Estabelecimento3"); //Optional
        spreadsheet.setHeader("Estabelecimento4"); //Optional
        spreadsheet.setHeader("Estabelecimento5"); //Optional
        spreadsheet.setHeader("Estabelecimento6"); //Optional
        spreadsheet.setHeader("Estabelecimento7"); //Optional
        spreadsheet.setHeader("Estabelecimento8"); //Optional
        spreadsheet.setHeader("Observacoes"); //Optional         
        spreadsheet.setHeader("DataGrauEmPortugal"); //56
        spreadsheet.setHeader("ClassificacaoID"); //57
        spreadsheet.setHeader("HandleDepositoRCAAP"); //Optional
        spreadsheet.setHeader("RegistoInternoID"); //Optional

        Set<CurriculumGroup> collect =
                Bennu.getInstance().getProgramConclusionSet().stream().flatMap(pc -> pc.getCourseGroupSet().stream())
                        .filter(cg -> cg.isCycleCourseGroup() && ((CycleCourseGroup) cg).isSecondCycle())
                        .flatMap(cg -> cg.getCurriculumModulesSet().stream()).map(cm -> (CurriculumGroup) cm)
                        .filter(cm -> cm.isConclusionProcessed()).collect(Collectors.toSet());

        final Set<Thesis> thesisSet = FenixFramework.getDomainRoot().getBennu().getThesesSet();
        Set<String> inExcel = new HashSet<String>();

        for (CurriculumGroup group : collect) {
            Enrolment thesisEnrolment = getThesisEnrolment(group);

            if (group.getStudent().getNumber().equals(69632)) {
                taskLog("WARN: %s %s%n", group.getExternalId(), group.getStudentCurricularPlan().getName());
            }

            if (thesisEnrolment == null) {
                continue;
            }

            Thesis thesis = thesisEnrolment.getPossibleThesis();
            ConclusionProcess conclusionProcess = group.getConclusionProcess();

            if (thesis == null) {
                thesis_is_null.add(group.getStudent().getNumber().toString());
                //taskLog("thesis is null: %s%n", group.getStudent().getNumber());                

                continue;
            }

            Locale locale = new Locale("pt", "PT");

            final Row row = spreadsheet.addRow();

            final Person person = thesis.getStudent().getPerson();

            if (!thesis.isFinalAndApprovedThesis()) {
                taskLog("ERROR!!!! %s %s%n", thesis.getExternalId(), person.getUsername());
            }

            row.setCell(0, person.getName()); //NomeCompleto
            row.setCell(1, person.getGender() == Gender.MALE ? "H" : "F"); //Sexo
            row.setCell(2, person.getDateOfBirthYearMonthDay().toString("dd-MM-yyyy")); //DataNascimento
            row.setCell(3, person.getDocumentIdNumber()); //NumIdentificacao

            final String studentIdType = idDocumentTypeToNumber(person.getIdDocumentType());

            row.setCell(4, studentIdType); //TipoIdentificacao
            row.setCell(5, studentIdType.equals("9") ? person.getIdDocumentType().getLocalizedName(locale) : ""); //OutroTipoIdentificacao
            row.setCell(6, person.getCountryOfBirth().getCode()); //PaisNacionalidade
            final EmailAddress email = person.getEmailAddressForSendingEmails();
            row.setCell(10, email == null ? "email not found" : email.getPresentationValue()); //Email      

            row.setCell(12, "1500"); //CodEstabelecimentoPortugues
            final DegreeCurricularPlan degreeCurricularPlan = thesis.getEnrolment().getDegreeCurricularPlanOfStudent();

            row.setCell(13, degreeCurricularPlan.getLastCampus().getName().equals("Alameda") ? "1518" : "1519"); //codUnidadeOrganica
            final Degree degree = degreeCurricularPlan.getDegree();

            row.setCell(14, degree.getMinistryCode()); //CodCurso
            row.setCell(15, degree.getPresentationName()); //Curso

            row.setCell(16, "1000017"); //CodEspecializacao
            row.setCell(17, "Sem especialização"); //Especializacao
            row.setCell(18, thesis.getTitle().getContent(locale)); //TituloTrabalho

            final Person orientator1 = getOrientor(thesis);

            if (orientator1 != null) {
                row.setCell(19, orientator1.getName()); //NomeOrientador1
                row.setCell(20, orientator1.getDocumentIdNumber()); //NumIdentificacao_Orientador1                
                final String orienter1IdType = idDocumentTypeToNumber(orientator1.getIdDocumentType());
                row.setCell(21, orienter1IdType); //TipoIdentificacao_Orientador1
                row.setCell(22, orienter1IdType.equals("9") ? orientator1.getIdDocumentType().getLocalizedName(locale) : ""); //OutroTipoIdentificacao_Orientador1

            }

            row.setCell(45, thesis.getKeywordsPt()); //Palavras_chave
            row.setCell(46, "Não"); //Curso_Parceria           
            row.setCell(56, conclusionProcess.getConclusionDate().toString("dd-MM-yyyy")); //DataGrauEmPortugal
            row.setCell(57, conclusionProcess.getFinalGrade().getValue()); //ClassificacaoID
            inExcel.add(thesis.getStudent().getNumber().toString());

        }
        printError(thesis_is_null, "thesis is null", inExcel, thesisSet);
        printError(enrolment_is_null, "curriculum Line was not found", inExcel, thesisSet);
        printError(no_dismissal_no_enrolment, "no dismissal no enrolment", inExcel, thesisSet);
        printError(dismissal_no_enrolment, "dismissal no enrolment", inExcel, thesisSet);
        printError(dismissal_more_enrolments, "dismissal more than 1 enrolment", inExcel, thesisSet);
        return spreadsheet;

    }

    public void printError(List<String> students, String message, Set<String> inExcel, Set<Thesis> thesisSet) {
        for (String student : students) {
            Set<Thesis> thesis2 = thesisSet.stream().filter(t -> t.getStudent().getNumber().toString().equals(student))
                    .collect(Collectors.toSet());
            String check = inExcel.contains(student) ? "Sim" : "Nao";
            String thesisState = "";
            String thesisEnrolmentState = "";
            for (Thesis thesis : thesis2) {
                if (thesisState.length() > 0) {
                    thesisState += ", ";
                }
                if (thesisEnrolmentState.length() > 0) {
                    thesisEnrolmentState += ", ";
                }

                thesisEnrolmentState += thesis.getEnrolment().getEnrollmentState().toString();
                thesisState += thesis.getState().toString();
            }
            taskLog("%s-> Student: %s Number of thesis: %s Is in excel: %s Thesis State: %s Thesis  Enrolment State: %s%n",
                    message, student, thesis2.size(), check, thesisState, thesisEnrolmentState);
        }
    }

    private Enrolment getThesisEnrolment(CurriculumGroup group) {
        Predicate<CurriculumLine> isConcludedEnrolment = e -> (e.isEnrolment() && ((Enrolment) e).isDissertation()
                && e.isApproved() && ((Enrolment) e).getPossibleThesis() != null
                && ((Enrolment) e).getPossibleThesis().isFinalAndApprovedThesis());

        Set<CurriculumLine> curriculumLines = group.getAllCurriculumLines().stream()
                .filter(e -> (e.isDismissal() && ((Dismissal) e).getCurricularCourse() != null
                        && ((Dismissal) e).getCurricularCourse().isDissertation()
                        && ((Dismissal) e).getCredits().isSubstitution()) || isConcludedEnrolment.test(e))
                .collect(Collectors.toSet());

        if (curriculumLines.isEmpty()) {
            no_dismissal_no_enrolment.add(group.getStudent().getNumber().toString());
            return null;
        }

        for (CurriculumLine curriculumLine : curriculumLines) {

            if (curriculumLine instanceof Dismissal) {
                Dismissal dismissal = (Dismissal) curriculumLine;
                Substitution substitution = (Substitution) dismissal.getCredits();
                Enrolment enrolment =
                        (Enrolment) substitution.getEnrolmentsSet().stream().filter(e -> e.getIEnrolment().isEnrolment())
                                .filter(e -> isConcludedEnrolment.test((CurriculumLine) e.getIEnrolment()))
                                .map(e -> e.getIEnrolment()).findAny().orElse(null);
                if (enrolment != null) {
                    return enrolment;
                }

                dismissal_no_enrolment.add(group.getStudent().getNumber().toString());
                continue;
            }
            if (curriculumLine instanceof Enrolment) {
                return (Enrolment) curriculumLine;
            }
        }
        enrolment_is_null.add(group.getStudent().getNumber().toString());
        return null;
    }

    /*private Enrolment getThesisEnrolment(CurriculumGroup group) {
        Predicate<CurriculumLine> isConcludedEnrolment = e -> (e.isEnrolment() && ((Enrolment) e).isDissertation()
                && e.isApproved() && ((Enrolment) e).getPossibleThesis() != null
                && ((Enrolment) e).getPossibleThesis().isFinalAndApprovedThesis());
    
        Optional<CurriculumLine> curriculumLineOpt =
                group.getAllCurriculumLines().stream()
                        .filter(e -> (e.isDismissal() && ((Dismissal) e).getCurricularCourse() != null
                                && ((Dismissal) e).getCurricularCourse().isDissertation()) || isConcludedEnrolment.test(e))
                        .findAny();
    
        if (!curriculumLineOpt.isPresent()) {
            no_dismissal_no_enrolment.add(group.getStudent().getNumber().toString());
            //taskLog("no dismissal no enrolment: %s%n", group.getStudent().getNumber());
            return null;
        }
    
        CurriculumLine curriculumLine = curriculumLineOpt.get();
    
        if (curriculumLine instanceof Dismissal) {
            Dismissal dismissal = (Dismissal) curriculumLine;
            if (dismissal.getCredits().isSubstitution()) {
                Substitution substitution = (Substitution) dismissal.getCredits();
                Enrolment enrolment =
                        (Enrolment) substitution.getEnrolmentsSet().stream().filter(e -> e.getIEnrolment().isEnrolment())
                                .filter(e -> isConcludedEnrolment.test((CurriculumLine) e.getIEnrolment()))
                                .map(e -> e.getIEnrolment()).findAny().orElse(null);
                if (enrolment != null) {
                    taskLog("Right-> curriculumLine: %s%n", curriculumLine.getExternalId());
                    return enrolment;
                }
            }
    
            dismissal_no_enrolment.add(group.getStudent().getNumber().toString());
            taskLog("Student: %s Cg: %s Curriculum line: %s%n", group.getStudent().getNumber().toString(), group.getExternalId(),
                    curriculumLine.getExternalId());
            return null;
        }
    
        return (Enrolment) curriculumLine;
    }*/

    public String idDocumentTypeToNumber(IDDocumentType idDocumentType) {

        switch (idDocumentType) {
        case IDENTITY_CARD:
            return "1";

        case CITIZEN_CARD:
            return "1";

        case PASSPORT:
            return "2";

        case RESIDENCE_AUTHORIZATION:
            return "3";

        case FOREIGNER_IDENTITY_CARD:
            return "4";

        case OTHER:
            return "9";

        case NATIVE_COUNTRY_IDENTITY_CARD:
            return "9";

        case NAVY_IDENTITY_CARD:
            return "9";

        case AIR_FORCE_IDENTITY_CARD:
            return "9";

        case MILITARY_IDENTITY_CARD:
            return "9";

        case EXTERNAL:
            return "9";

        default:
            return "9";
        }
    }

    public Person getOrientor(Thesis thesis) {
        for (ThesisEvaluationParticipant thesisEvaluationParticipant : thesis
                .getAllParticipants(ThesisParticipationType.ORIENTATOR)) {
            Person orientator = thesisEvaluationParticipant.getPerson();
            if (orientator != null) {
                return orientator;
            }
        }

        return null;

    }
}