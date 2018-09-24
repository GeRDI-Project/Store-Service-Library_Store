/**
 * Copyright © 2018 Nelson Tavares de Sousa (tavaresdesousa@email.uni-kiel.de)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.store.datamodel;

public class TaskElement {

    private final String fileName;
    private final transient Progress parent;
    private Integer progressInPercent = Integer.valueOf(0);

    /**
     * The TaskElement constructor
     * @param fileName The name of the file to be copied
     * @param parent The parent instance containing this TaskElement
     */
    TaskElement(final String fileName, final Progress parent) {
        this.fileName = fileName;
        this.parent = parent;
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getProgressInPercent() {
        return progressInPercent;
    }

    public void setProgressInPercent(final Integer progressInPercent) {
        this.progressInPercent = progressInPercent;
        if (progressInPercent == 100) {
            this.parent.notifyFinishedCopy();
        }
    }


}
