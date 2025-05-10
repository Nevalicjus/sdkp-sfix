public class Main {
    public static void main(String[] args) {
        String fp = "./tests/tpo2_kj_s12345.zip";
        SDKPSubmissionFixer.fixSubmission(
            new SDKPSubmissionFixer.SubmissionFile("tpo", 2, "Jan", "Kowalski", "s12345"),
            fp
        );
    }
}
