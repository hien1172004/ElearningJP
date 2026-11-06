package com.jp.elearningjp.service.curriculum.importer.grammar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Chuẩn hoá tên ngữ pháp khi import: các cách gõ khác nhau phải cho cùng một khoá. */
class GrammarTitleNormalizeTest {

    @Test
    void waveDash_fullwidthTilde_asciiTilde_areEquivalent() {
        String expected = GrammarImportService.normalizeTitle("〜てもいい");
        assertThat(GrammarImportService.normalizeTitle("～てもいい")).isEqualTo(expected);
        assertThat(GrammarImportService.normalizeTitle("~てもいい")).isEqualTo(expected);
    }

    @Test
    void parentheses_spaces_case_areIgnored() {
        String expected = GrammarImportService.normalizeTitle("〜に（time）");
        assertThat(GrammarImportService.normalizeTitle("～に(Time)")).isEqualTo(expected);
        assertThat(GrammarImportService.normalizeTitle(" ~に (time) ")).isEqualTo(expected);
    }

    @Test
    void slashTitles_matchWithOrWithoutSpaces() {
        assertThat(GrammarImportService.normalizeTitle("あげる/くれる/もらう"))
                .isEqualTo(GrammarImportService.normalizeTitle("あげる / くれる / もらう"));
    }

    @Test
    void differentGrammar_staysDifferent() {
        assertThat(GrammarImportService.normalizeTitle("〜に（time）"))
                .isNotEqualTo(GrammarImportService.normalizeTitle("〜に（destination）"));
    }
}
