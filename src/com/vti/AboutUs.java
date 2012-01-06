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
package com.vti;

import java.util.regex.Pattern;

import com.vti.R;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;


public class AboutUs extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_us);
		TextView aboutUs = (TextView) findViewById(R.id.about_us);
		final String aboutUsText="VTI is developed and currently maintained by Sol Ma, a graduate student from University of Illinois at Chicago. For more information about VTI, please visit <a href='http://www.cs.uic.edu/~sma/VTI'>the VTI homepage</a>. For comments/questions regarding VTI, please write feedbacks or contact us at: sma21@uic.edu.";
		//jmt: pattern we want to match and turn into a clickable link
		//Pattern pattern = Pattern.compile("the VTI homepage");
		//jmt: prefix our pattern with http://
		//Linkify.addLinks(aboutUs, pattern, "http://");
		aboutUs.setMovementMethod(LinkMovementMethod.getInstance());
		aboutUs.setText(Html.fromHtml(aboutUsText));
	}

}
