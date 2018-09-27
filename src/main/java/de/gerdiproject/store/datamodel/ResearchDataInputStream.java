/**
 * Copyright © 2018 Nelson Tavares de Sousa (tavaresdesousa@email.uni-kiel.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.store.datamodel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * This class implements an {@linkplain InputStream} for research data while added library specific logic.
 */
public class ResearchDataInputStream extends InputStream {

    private final long size;
    private final InputStream inputStream;
    private final StoreTask parent;
    private final String name;
    private long copiedSize = 0;
    private CopyStatus status = CopyStatus.PENDING;

    public ResearchDataInputStream(final URL url, final StoreTask storeTask) throws IOException {
        super();
        this.name = url.getFile();
        this.inputStream = url.openStream();
        this.size = url.openConnection().getContentLengthLong();
        this.parent = storeTask;
        if (this.size == -1) {
            this.status = CopyStatus.UNKNOWN_SIZE;
        }
    }

    @Override
    public int read() throws IOException {
        this.copiedSize++;
        return inputStream.read();
    }

    /**
     * Returns the name of the file.
     * @return The file name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the progress as percentage represented as int.
     * @return The progress value
     */
    public int getProgressInPercent(){
        if (status == CopyStatus.ERROR || status == CopyStatus.UNKNOWN_SIZE) {
            return 0;
        }
        return (int) (copiedSize * 100 / size);
    }

    /**
     * Returns the state of this stream
     * @return a {@linkplain CopyStatus} depicting the state
     */
    public CopyStatus getStatus() {
        return this.status;
    }

    /**
     * Setter for the state
     * @param status the new state
     */
    public void setStatus(final CopyStatus status) {
        this.status = status;
    }

}
