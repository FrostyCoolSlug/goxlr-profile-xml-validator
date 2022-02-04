import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;

public class Runner {
    public static void main(String args[]) throws FileNotFoundException, SAXException, IOException {
        Runner.compareXML();
    }

    public static void compareXML() throws SAXException, IOException {
        Diff diff = DiffBuilder.compare(Input.fromFile("profile.xml"))
                .withTest(Input.fromFile("output.xml"))
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
                .ignoreWhitespace()
                .build();

        Iterator<Difference> iter = diff.getDifferences().iterator();
        while (iter.hasNext()) {
            Difference difference = iter.next();
            if (difference.getResult() == ComparisonResult.SIMILAR) {
                // Our tags and attributes may be out of order, this results in a 'SIMILAR' result, we don't care.
                continue;
            } else {
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
                    } catch (NumberFormatException ignored) {}
                }

                // Something is different!
                System.out.println(difference.toString());
            }
        }
    }
}
