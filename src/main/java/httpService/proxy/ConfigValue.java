package httpService.proxy;

class ConfigValue {
        private int index;
        private boolean require;
        private String defaultValue;

        int getIndex() {
            return this.index;
        }

        boolean isRequire() {
            return this.require;
        }

        String getDefaultValue() {
            return this.defaultValue;
        }

        void setIndex(int index) {
            this.index = index;
        }

        void setRequire(boolean require) {
            this.require = require;
        }

        void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }