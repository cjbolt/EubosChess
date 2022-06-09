package eubos.main;

class CommandPair {
	protected String in;
	protected String out;
	public CommandPair(String input, String output) {
		in = input;
		out = output;
	}		
	public String getIn() {
		return in;
	}
	public String getOut() {
		return out;
	}
	public boolean expectOutput() {
		return out != null;
	}
	public boolean isExpectedOutput(String received) {
		return received.endsWith(out);
	}
}

class MultipleAcceptableCommandPair extends CommandPair {
	private String [] outputAlternatives;
	public MultipleAcceptableCommandPair(String input, String [] output) {
		super(input, null);
		outputAlternatives = output;
	}
	@Override
	public boolean expectOutput() {
		return outputAlternatives != null;
	}
	@Override
	public boolean isExpectedOutput(String received) {
		boolean expectedCommandReceived = false;
		for (String command : outputAlternatives) {
			if (received.endsWith(command)) {
				expectedCommandReceived = true;
				break;
			}
		}
		return expectedCommandReceived;
	}
}
