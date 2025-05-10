#!/usr/bin/env python3
import os
import zipfile

cleanup = True
show_contents = False
testroot = "./tests/"

def main() -> None:
    print("Running... ")
    os.system(f"cp SDKPSubmissionFixer.java {testroot}")

    tests = [
        test_missing_dots,
        test_no_root,
        test_authors,
        test_dir_file,
        test_file_dir
    ]
    longest_name = max([len(x.__name__) for x in tests])

    tests_passed = 0
    try:
        for fn in tests:
            passed = fn()
            tests_passed += 1 if passed else 0
            status = "\033[92mok\033[0m" if passed else "\033[91mfailed\033[0m"
            print(f"{fn.__name__: <{longest_name}} {status}")
    except Exception as e:
        print(f"Exception during execution: {e}")

    os.system(f"cd {testroot} && rm Test.java *.class SDKPSubmissionFixer.java 2>/dev/null")
    print(f"Finished - {tests_passed} / {len(tests)}")

# -- Tests

def test_missing_dots() -> bool:
    fp = "tpo2_kj_s12345"
    os.system(f"cd {testroot} && rm {fp}.zip 2>/dev/null ; zip -r {fp}.zip {fp} 1>/dev/null")
    fp += ".zip"
    zfs = [
        "TPO2_KJ_S12345/",
        "TPO2_KJ_S12345/.project",
        "TPO2_KJ_S12345/.classpath",
        "TPO2_KJ_S12345/src/",
        "TPO2_KJ_S12345/src/Utils.java",
        "TPO2_KJ_S12345/src/Main.java",
        "TPO2_KJ_S12345/lib/"
    ]
    cr_java(("tpo", 2, "Jan", "Kowalski", "s12345"))
    ans = zips_equal(fp, zfs)
    if cleanup: os.system(f"cd {testroot} && rm {fp}")
    return ans

def test_no_root() -> bool:
    fp = "utp2_kj_s12345"
    os.system(f"cd {testroot} && rm {fp}.zip 2>/dev/null ; cp {fp}.orig.zip {fp}.zip 1>/dev/null")
    fp += ".zip"
    zfs = [
        "UTP2_KJ_S12345/",
        "UTP2_KJ_S12345/.project",
        "UTP2_KJ_S12345/.classpath",
        "UTP2_KJ_S12345/Main.java",
        "UTP2_KJ_S12345/readme.txt"
    ]
    cr_java(("utp", 2, "Jan", "Kowalski", "s12345"))
    ans = zips_equal(fp, zfs)
    if cleanup: os.system(f"cd {testroot} && rm {fp}")
    return ans

def test_dir_file() -> bool:
    fp = "tpo5_kj_s12345"
    os.system(f"cd {testroot} && rm {fp}.zip 2>/dev/null ; cp {fp}.orig.zip {fp}.zip 1>/dev/null")
    fp += ".zip"
    zfs = [
        "TPO5_KJ_S12345/",
        "TPO5_KJ_S12345/.project",
        "TPO5_KJ_S12345/.classpath",
        "TPO5_KJ_S12345/Test.java",
        "TPO5_KJ_S12345/src/"
    ]
    cr_java(("tpo", 5, "Jan", "Kowalski", "s12345"))
    ans = zips_equal(fp, zfs)
    if cleanup: os.system(f"cd {testroot} && rm {fp}")
    return ans

def test_file_dir() -> bool:
    fp = "tpo6_kj_s12345"
    os.system(f"cd {testroot} && rm {fp}.zip 2>/dev/null ; cp {fp}.orig.zip {fp}.zip 1>/dev/null")
    fp += ".zip"
    zfs = [
        "TPO6_KJ_S12345/",
        "TPO6_KJ_S12345/.project",
        "TPO6_KJ_S12345/.classpath",
        "TPO6_KJ_S12345/Test.java",
        "TPO6_KJ_S12345/src/"
    ]
    cr_java(("tpo", 6, "Jan", "Kowalski", "s12345"))
    ans = zips_equal(fp, zfs)
    if cleanup: os.system(f"cd {testroot} && rm {fp}")
    return ans

def test_authors() -> bool:
    fp = "tpo3_kj_s12345"
    os.system(f"cd {testroot} && rm {fp}.zip 2>/dev/null ; zip -r {fp}.zip {fp} 1>/dev/null")
    fp += ".zip"
    zfs = [
        "TPO3_KJ_S12345/",
        "TPO3_KJ_S12345/.project",
        "TPO3_KJ_S12345/.classpath",
        "TPO3_KJ_S12345/src/",
        "TPO3_KJ_S12345/src/Test1.java",
        "TPO3_KJ_S12345/src/Test2.java",
        "TPO3_KJ_S12345/src/Test3.java",
        "TPO3_KJ_S12345/src/Test4.java",
        "TPO3_KJ_S12345/src/Test5.java"
    ]
    good_author = "/**\n *\n *  @author Kowalski Jan S12345\n *\n *\n */\n"
    authors = [
        ("Test1.java", good_author + "/*\n*\n* @author Jan Kowalski S12345\n*\n*\n*/\n"),       # bad - no spaces, comment
        ("Test2.java", good_author + "/*\n *\n * @author Jan Kowalski S12345\n *\n *\n */\n"),  # bad - spaces, comment
        ("Test3.java", good_author + "/*\n*\n*/\npublic class Test3 {}\n"),                     # bad - comment
        ("Test4.java",               "/**\n*\n*  @author Jan Kowalski S12345\n*\n*\n*/\n"),     # good - no spaces
        ("Test5.java",               "/**\n *\n *  @author Jan Kowalski S12345\n *\n *\n */\n") # good - spaces
    ]
    cr_java(("tpo", 3, "Jan", "Kowalski", "s12345"))
    ans = zips_equal(fp, zfs)
    for (idx, (file, content)) in enumerate(authors):
        ans = ans and read_zip_contents(fp, f"TPO3_KJ_S12345/src/{file}") == content
    if cleanup: os.system(f"cd {testroot} && rm {fp}")
    return ans

# -- Utils
def cr_java(conf: tuple[str, int, str, str, str], fp: str = "Test") -> None:
    subj, nlab, name, surn, sidx = conf
    java_src = f"""
public class Test {{
    public static void main(String[] args) {{
        String fp = "{fp}";
        SDKPSubmissionFixer.fixSubmission(
            new SDKPSubmissionFixer.SubmissionFile("{subj}", {nlab}, "{name}", "{surn}", "{sidx}"), fp
        );
    }}
}}
"""
    with open(f"{testroot}{fp}.java", "w") as f:
        f.write(java_src)
    os.system(f"cd {testroot} && javac {fp}.java && java {fp} && rm {fp}.java *.class")

def zips_equal(z1: str, z2: list[str]) -> bool:
    z1 = [x.filename for x in zipfile.ZipFile(testroot + z1).filelist]
    if show_contents:
        for x in sorted(z1):
            print(x)
    return sorted(z1) == sorted(z2)

def read_zip_contents(z1: str, fp: str) -> str:
    z = zipfile.ZipFile(testroot + z1)
    with z.open(fp, "r") as f:
        ans = f.read().decode("utf-8")
    return ans
# --

if __name__ == "__main__":
    main()