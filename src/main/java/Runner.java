import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;

public class Runner {
    public static void main(String[] args) {
        System.out.println("Beginning Validation..");
        int errors = Runner.compareXML();
        System.out.println();
        System.out.println("Validation Complete, " + errors + " problems found.");
    }

    public static int compareXML() {
        Diff diff = DiffBuilder.compare(Input.fromFile("profile.xml"))
                .withTest(Input.fromFile("output.xml"))
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
                .ignoreWhitespace()
                .build();

        Iterator<Difference> iter = diff.getDifferences().iterator();
        var errors = 0;
        while (iter.hasNext()) {
            Difference difference = iter.next();
            if (difference.getResult() != ComparisonResult.SIMILAR) {
                // Do we have different Attribute Values?
                if (difference.getComparison().getType() == ComparisonType.ATTR_VALUE) {
                    String controlText = (String) difference.getComparison().getControlDetails().getValue();
                    String testText = (String) difference.getComparison().getTestDetails().getValue();

                    // More importantly, does it matter in this context?
                    if (difference.getComparison().getControlDetails().getXPath().matches(".*colour[0-9]")) {
                        // If the case is different, ignore it..
                        if (controlText.equalsIgnoreCase(testText)) {
                            continue;
                        }
                    }

                    // Between Version 1 and 2, the 'muteFunction' attribute value was changed from 'All' to 'Mute All'
                    // but the GoXLR UI will persist the old value if present, I change it.
                    if (difference.getComparison().getControlDetails().getXPath().matches(".*mute[0-9]Function")) {
                        if (controlText.equals("All") && testText.equals("Mute All")) {
                            continue;
                        }
                    }

                    // Have we simply rounded a broken or extreme float?
                    try {
                        BigDecimal origin = new BigDecimal(controlText);
                        BigDecimal target = new BigDecimal(testText);

                        if (origin.compareTo(target) == 0) {
                            // Catch cases like 5.00000000000000 -> 5
                            continue;
                        }

                        // Round the origin to the target's DP and compare..
                        int decimalPlaces = testText.length() - testText.indexOf(".") - 1;
                        BigDecimal scaledValue = origin.setScale(decimalPlaces, RoundingMode.HALF_UP);

                        if (scaledValue.compareTo(target) == 0) {
                            continue;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }

                if (difference.getComparison().getType() == ComparisonType.ELEMENT_NUM_ATTRIBUTES) {
                    // There's a specific case when moving from v1 to v2 config where this would be valid (the attribute goes away)
                    if (difference.getComparison().getControlDetails().getXPath().matches(".*sampleStack[A-C].*")) {
                        if (((int) difference.getComparison().getControlDetails().getValue() == 1) &&
                                ((int) difference.getComparison().getTestDetails().getValue() == 0)) {
                            continue;
                        }
                    }
                }

                if (difference.getComparison().getType() == ComparisonType.ATTR_NAME_LOOKUP) {
                    if (difference.getComparison().getControlDetails().getXPath().matches(".*sampleStack[A-C]stackSize")) {
                        System.out.println();
                        System.out.println("The following error can occur when upgrading a v1 profile to v2, if that's the case, ignore.");
                    }
                }

                // Something is different!
                System.out.println(difference);
                errors++;
            }
        }
        return errors;
    }
}
