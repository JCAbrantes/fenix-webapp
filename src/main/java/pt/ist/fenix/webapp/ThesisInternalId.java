package pt.ist.fenix.webapp;

import java.io.IOException;
import java.util.Locale;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixframework.Atomic.TxMode;

public class ThesisInternalId extends CustomTask {

    private final static Locale PT = new Locale("pt", "PT");

    @Override
    public TxMode getTxMode() {

        return TxMode.READ;

    }

    @Override
    public void runTask() throws IOException {
        //checkThesisId();
    }

    /*public void checkThesisId() {
        Set<Thesis> thesisSet = Bennu.getInstance().getThesesSet();
        Set<String> thesisIds = new HashSet<String>();
        for (Thesis thesis : getRenatesThesis()) {
    
            String id = getThesisId(thesis);
    
            if (thesisIds.contains(id)) {
                taskLog(id);
                for (Thesis thesis2 : thesisSet) {
                    if (getThesisId(thesis2).equals(id)) {
                        taskLog("        thesisId: %s%n", thesis2.getExternalId());
                    }
    
                }
            }
            thesisIds.add(id);
    
        }
    
    }
    
    public String getThesisId(Thesis thesis) {
        String istId = thesis.getStudent() == null ? thesis.getExternalId() : thesis.getStudent().getPerson().getUsername();
        String course = thesis.getEnrolment() == null ? thesis.getExternalId() : thesis.getEnrolment()
                .getDegreeCurricularPlanOfStudent().getDegree().getSigla();
        String approval =
                thesis.getApproval() == null ? thesis.getExternalId() : Integer.toString(thesis.getApproval().getYear());
    
        return Joiner.on("/").join(approval, course, istId);
    }
    
    public Set<Thesis> getRenatesThesis() {
        Set<Thesis> thesisSet = new HashSet<Thesis>();
        Set<CurriculumGroup> collect =
                Bennu.getInstance().getProgramConclusionSet().stream().flatMap(pc -> pc.getCourseGroupSet().stream())
                        .filter(cg -> cg.isCycleCourseGroup() && ((CycleCourseGroup) cg).isSecondCycle())
                        .flatMap(cg -> cg.getCurriculumModulesSet().stream()).map(cm -> (CurriculumGroup) cm)
                        .filter(cm -> cm.isConclusionProcessed()).collect(Collectors.toSet());
    
        for (CurriculumGroup group : collect) {
            Enrolment thesisEnrolment = getThesisEnrolment(group);
            if (thesisEnrolment == null) {
                continue;
            }
            Thesis thesis = thesisEnrolment.getPossibleThesis();
            ConclusionProcess conclusionProcess = group.getConclusionProcess();
    
            if (thesis == null) {
                continue;
            }
            if (thesis.getThesisId() != null && thesis.getThesisId().getId() != null) {
                continue;
            }
            thesisSet.add(thesis);
    
        }
    
        return thesisSet;
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
            }
            if (curriculumLine instanceof Enrolment) {
                return (Enrolment) curriculumLine;
            }
        }
        return null;
    }*/
}
