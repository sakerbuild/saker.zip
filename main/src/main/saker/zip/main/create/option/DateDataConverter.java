/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package saker.zip.main.create.option;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import saker.build.task.utils.StructuredTaskResult;
import saker.build.util.data.ConversionContext;
import saker.build.util.data.ConversionFailedException;
import saker.build.util.data.DataConverter;
import saker.build.thirdparty.saker.util.io.IOUtils;

public class DateDataConverter implements DataConverter {
	public static final SimpleDateFormat PARSER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	public static final SimpleDateFormat PARSER_NOMILLIS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final SimpleDateFormat PARSER_NOSEC = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	public static final SimpleDateFormat PARSER_DATE1 = new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat PARSER_DATE2 = new SimpleDateFormat("yyyy.MM.dd");

	public static final SimpleDateFormat[] PARSERS = { PARSER, PARSER_NOMILLIS, PARSER_NOSEC, PARSER_DATE1,
			PARSER_DATE2 };

	@Override
	public Object convert(ConversionContext conversioncontext, Object value, Type targettype)
			throws ConversionFailedException {
		if (value == null) {
			return null;
		}
		if (value instanceof StructuredTaskResult) {
			value = ((StructuredTaskResult) value).toResult(conversioncontext.getTaskResultResolver());
		}
		if (value instanceof Date) {
			return value;
		}
		if (value instanceof Instant) {
			Instant instant = (Instant) value;
			return new Date(instant.toEpochMilli());
		}
		String str = value.toString();
		if (str == null) {
			return null;
		}
		ParseException exc = null;
		for (SimpleDateFormat parser : PARSERS) {
			try {
				return parser.parse(str);
			} catch (ParseException e) {
				exc = IOUtils.addExc(exc, e);
			}
		}
		throw new ConversionFailedException("Failed to parse to Date: " + value, exc);
	}

}