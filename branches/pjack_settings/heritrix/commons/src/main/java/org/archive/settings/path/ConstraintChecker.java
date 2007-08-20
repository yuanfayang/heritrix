/**
 * 
 */
package org.archive.settings.path;

import java.util.List;
import java.util.Map;

import org.archive.settings.Offline;
import org.archive.settings.Sheet;
import org.archive.settings.SingleSheet;
import org.archive.state.Constraint;
import org.archive.state.Key;
import org.archive.state.KeyManager;

public final class ConstraintChecker extends StringPathListConsumer {
    private final List<PathChangeException> problems;
    private final SingleSheet r;
    private Constraint<?> faulty;

    public ConstraintChecker(List<PathChangeException> problems,
            SingleSheet r) {
        this.problems = problems;
        this.r = r;
    }

    @Override
    public void consume(String path, List<Sheet> sheet,
            Object value, Class type, String seenPath) {
        if (value instanceof Offline) {
            return;
        }
        int p = path.lastIndexOf(PathValidator.DELIMITER);
        if (p < 0) {
            return;
        }
        String parentPath = path.substring(0, p);
        Object parent = PathValidator.validate(r, parentPath);
        if (parent instanceof List) {
            return;
        }
        if (parent instanceof Map) {
            return;
        }
        String keyName = path.substring(p + 1);
        Key<Object> key = KeyManager.getKeys(Offline.getType(parent)).get(keyName);
        for (Constraint<Object> c: key.getConstraints()) {
            if (!c.allowed(value)) {
                faulty = c;
            }
        }
        super.consume(path, sheet, value, type, seenPath);
    }

    @Override
    protected void consume(String path, String[] sheets,
            String value, String type) {
        if (faulty != null) {
            PathChangeException pce = new PathChangeException(faulty.toString());
            pce.setPathChange(new PathChange(path, type, value));
            problems.add(pce);
        }
        faulty = null;
    }
}