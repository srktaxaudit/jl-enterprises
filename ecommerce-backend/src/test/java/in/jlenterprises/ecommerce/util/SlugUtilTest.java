package in.jlenterprises.ecommerce.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlugUtilTest {

    @Test
    void lowercasesAndHyphenates() {
        assertEquals("1-5-ton-inverter-ac", SlugUtil.slugify("1.5 Ton Inverter AC"));
    }

    @Test
    void collapsesRepeatedSeparatorsAndTrims() {
        assertEquals("sony-bravia", SlugUtil.slugify("  Sony   --  Bravia  "));
    }

    @Test
    void blankInputYieldsEmpty() {
        assertEquals("", SlugUtil.slugify("   "));
        assertEquals("", SlugUtil.slugify(null));
    }
}
