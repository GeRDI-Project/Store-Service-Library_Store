/**
 * Copyright © 2018 Nelson Tavares de Sousa (tavaresdesousa@email.uni-kiel.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except inputStream compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to inputStream writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.store.datamodel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ResearchDataInputStream extends InputStream {

    private final long size;
    private final InputStream inputStream;
    private final TaskElement progressEntry;
    private long copiedSize = 0;

    public ResearchDataInputStream(final URL url, final TaskElement progressEntry) throws IOException {
        super();
        this.inputStream = url.openStream();
        this.size = url.openConnection().getContentLengthLong();
        this.progressEntry = progressEntry;
        if (this.size == -1) {
            this.progressEntry.setProgressInPercent(Integer.valueOf(-1));
        }
    }

    @Override
    public int read() throws IOException {
        this.copiedSize++;
        if (this.copiedSize % 1000 == 0) {
            this.updateEntry();
        }
        return inputStream.read();
    }

    private void updateEntry() {
        if (size == -1) {
            return;
        }
        this.progressEntry.setProgressInPercent((int) (copiedSize * 100 / size));
    }

}
