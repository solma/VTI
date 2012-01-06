/**
 * Copyright 2011 Sol Ma
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License.
 */
package com.vti.managers;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import com.vti.utils.Log;
class FlushedInputStream extends FilterInputStream {

	/**
	 * The constructor that takes in the InputStream reference.
	 * 
	 * @param inputStream
	 *            the input stream reference.
	 */
	public FlushedInputStream(final InputStream inputStream) {
		super(inputStream);
	}

	/**
	 * Overriding the skip method to actually skip n bytes. This implementation
	 * makes sure that we actually skip the n bytes no matter what.
	 * {@inheritDoc}
	 */
	@Override
	public long skip(final long n) throws IOException {
		long totalBytesSkipped = 0L;
		// If totalBytesSkipped is equal to the required number
		// of bytes to be skipped i.e. "n"
		// then come out of the loop.
		while (totalBytesSkipped < n) {
			// Skipping the left out bytes.
			long bytesSkipped = in.skip(n - totalBytesSkipped);
			// If number of bytes skipped is zero then
			// we need to check if we have reached the EOF
			if (bytesSkipped == 0L) {
				// Reading the next byte to find out whether we have reached
				// EOF.
				int bytesRead = read();
				// If bytes read count is less than zero (-1) we have reached
				// EOF.
				// Cant skip any more bytes.
				if (bytesRead < 0) {
					break; // we reached EOF
				} else {
					// Since we read one byte we have actually
					// skipped that byte hence bytesSkipped = 1
					bytesSkipped = 1; // we read one byte
				}
			}
			// Adding the bytesSkipped to totalBytesSkipped
			totalBytesSkipped += bytesSkipped;
		}
		return totalBytesSkipped;
	}
}
