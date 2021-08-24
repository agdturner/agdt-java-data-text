/*
 * Copyright 2021 Andy Turner, University of Leeds.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.leeds.ccg.data.text.io;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import uk.ac.leeds.ccg.generic.io.Generic_Files;
import uk.ac.leeds.ccg.data.text.core.Text_Strings;
import uk.ac.leeds.ccg.generic.io.Generic_Defaults;

/**
 * This class if for managing files for the library.
 */
public class Text_Files extends Generic_Files {

    private Path inDir;
    private Path outDir;

    public Text_Files(Path dataDir) throws IOException {
        super(new Generic_Defaults(dataDir));
    }

    /**
     * @return the inDir
     */
    public Path getInDir() throws IOException {
        if (inDir == null) {
            inDir = Paths.get(getInputDir().toString(), 
                    Text_Strings.s_LexisNexis);
        }
        return inDir;
    }

    /**
     * @return the inDir
     */
    public Path getOutDir() throws IOException {
        if (outDir == null) {
            outDir = Paths.get(getOutputDir().toString(), 
            Text_Strings.s_LexisNexis);
        }
        return outDir;
    }
}
