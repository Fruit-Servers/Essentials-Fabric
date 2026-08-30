package net.essentialsx.fabric.textreader;

import java.util.List;

public interface ITranslatableText extends IResolvable {
    List<TranslatableText> getLines();

    default int getLineCount() {
        return getLines().size();
    }

    final class TranslatableText {
        private final String key;
        private final Object[] args;

        public TranslatableText(final String key, final Object[] args) {
            this.key = key;
            this.args = args;
        }

        public String getKey() {
            return key;
        }

        public Object[] getArgs() {
            return args;
        }
    }
}
