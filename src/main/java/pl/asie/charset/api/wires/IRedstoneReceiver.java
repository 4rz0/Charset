/*
 * Copyright (c) 2015-2016 Adrian Siekierka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.asie.charset.api.wires;

/**
 * Implement this class as a capability if you want to
 * receive signals from redstone cables. Keep in mind, however,
 * that this does not replace vanilla redstone APIs.
 */
public interface IRedstoneReceiver {
	/**
	 * This function will be called when a connected redstone cable changes.
	 */
	void onRedstoneInputChange();
}
