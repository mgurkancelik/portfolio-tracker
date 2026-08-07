package com.portfoliotracker.backend.transaction;

import java.math.BigDecimal;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class PlainBigDecimalSerializer extends StdSerializer<BigDecimal> {

	public PlainBigDecimalSerializer() {
		super(BigDecimal.class);
	}

	@Override
	public void serialize(BigDecimal value, JsonGenerator generator, SerializationContext context) throws JacksonException {
		if (value == null) {
			generator.writeNull();
			return;
		}
		generator.writeNumber(value.toPlainString());
	}
}
