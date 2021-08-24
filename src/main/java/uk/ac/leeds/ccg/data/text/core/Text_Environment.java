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
package uk.ac.leeds.ccg.data.text.core;

import java.io.IOException;
import uk.ac.leeds.ccg.generic.core.Generic_Environment;
import uk.ac.leeds.ccg.generic.io.Generic_Defaults;

/**
 *
 * @author geoagdt
 */
public class Text_Environment  {
    
    public transient Generic_Environment env;
    
    public Text_Environment() throws IOException, Exception {
        env = new Generic_Environment(new Generic_Defaults());
    }
    
}
