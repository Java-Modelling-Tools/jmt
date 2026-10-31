import os, tempfile, unittest
from tools import coverage_matrix as cm

HDR = "GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED\n"


def row(pkg, cls, bm, bc, lm, lc, mm=0, mc=1):
    return f"g,{pkg},{cls},0,0,{bm},{bc},{lm},{lc},0,0,{mm},{mc}\n"


class T(unittest.TestCase):
    def setUp(self):
        self.d = tempfile.mkdtemp()

    def module(self, name, rows):
        p = os.path.join(self.d, name, "target", "site", "jacoco")
        os.makedirs(p)
        with open(os.path.join(p, "jacoco.csv"), "w") as f:
            f.write(HDR + "".join(rows))
        return os.path.join(p, "jacoco.csv")

    def test_load_module_sums_classes_per_package(self):
        p = self.module("m", [row("a", "A", 1, 2, 3, 4), row("a", "B", 1, 1, 1, 1), row("b", "C", 0, 0, 0, 5)])
        data = cm.load_module(p)
        self.assertEqual(data["a"], cm.Counts(4, 5, 2, 3, 0, 2))
        self.assertEqual(set(data), {"a", "b"})

    def test_percentages_use_covered_over_total(self):
        p = self.module("m", [row("a", "A", 0, 0, 1, 3)])
        self.assertIn("75.0%", cm.format_matrix({"m": cm.load_module(p)}))

    def test_zero_branches_prints_dash_not_division_error(self):
        p = self.module("m", [row("a", "A", 0, 0, 1, 3)])
        self.assertIn("-", cm.format_matrix({"m": cm.load_module(p)}))

    def test_jmt_app_and_integration_tests_are_skipped(self):
        for m in ("jmt-app", "jmt-integration-tests", "x"):
            self.module(m, [row("a", "A", 0, 0, 0, 1)])
        self.assertEqual(list(cm.collect(self.d)), ["x"])

    def test_module_without_csv_reports_no_unit_tests(self):
        os.makedirs(os.path.join(self.d, "e"))
        open(os.path.join(self.d, "e", "pom.xml"), "w").close()
        out = cm.format_matrix(cm.collect(self.d))
        self.assertIn("no unit tests", out)

    def test_diff_shows_before_and_after(self):
        old = {"m": {"a": cm.Counts(1, 1, 0, 0, 0, 1)}}
        new = {"m": {"a": cm.Counts(0, 2, 0, 0, 0, 1)}}
        out = cm.diff_matrix(old, new)
        self.assertIn("50.0%", out)
        self.assertIn("100.0%", out)

    def test_worst_classes_orders_by_missed_branches(self):
        p = self.module("m", [row("a", "A", 1, 0, 9, 0), row("a", "B", 5, 0, 1, 0), row("b", "C", 5, 0, 2, 0)])
        w = cm.worst_classes(p, 2)
        self.assertEqual([c for c, _, _ in w], ["b.C", "a.B"])
        self.assertEqual(cm.worst_classes(p, 5, "a")[0][0], "a.B")


if __name__ == "__main__":
    unittest.main()
