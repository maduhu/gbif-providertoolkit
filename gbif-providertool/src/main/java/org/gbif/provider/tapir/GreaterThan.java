package org.gbif.provider.tapir;

public class GreaterThan extends ComparisonOperator {

	public GreaterThan() {
		log.debug("Creating " + this.getClass().getSimpleName());
	}
	
	@Override
	protected String getOperatorSymbol() {
		return ">";
	}

	public boolean evaluate() {
		// TODO Auto-generated method stub
		return false;
	}

	public String toHQL() {
		// TODO Auto-generated method stub
		return null;
	}

}
