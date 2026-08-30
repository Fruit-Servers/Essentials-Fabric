package net.essentialsx.fabric.textreader;

import java.util.List;
import java.util.Map;

public interface IText extends IResolvable {
    List<String> getLines();

    List<String> getChapters();

    Map<String, Integer> getBookmarks();

    default int getLineCount() {
        return getLines().size();
    }
}
