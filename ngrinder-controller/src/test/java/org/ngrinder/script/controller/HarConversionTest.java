/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ngrinder.script.controller;

import org.junit.Test;
import org.ngrinder.script.service.FileEntryService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.Assert.assertNotNull;

public class HarConversionTest {

	private FileEntryService fileEntryService = new FileEntryService();

	@Test
	public void testConvertFileUpload() throws Exception {
		ClassPathResource resource = new ClassPathResource("www.ngrinder.har");
		MultipartFile upFile = new MockMultipartFile("www.ngrinder.har", resource.getInputStream());
		String har = fileEntryService.loadHAR(upFile, true);
		assertNotNull(har);
	}

}
