/**
 * 
 */
package codemining.java.tokenizers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;

import codemining.java.codeutils.IdentifierPerType;
import codemining.util.SettingsLoader;

/**
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class JavaTokenizerSomeTokens extends JavaTokenizer {
	public static final String GENERIC_IDENTIFIER = "%IDENTIFIER%";

	private static final long serialVersionUID = -8566029315110514304L;

	private static final Logger LOGGER = Logger
			.getLogger(JavaTokenizerSomeTokens.class.getName());

	private Set<String> methodIds;
	private Set<String> typeIds;
	private Set<String> varIds;

	private final boolean REMOVE_METHOD_IDENTIFIERS = SettingsLoader
			.getBooleanSetting("removeMethodIdentifiers", false);
	private final boolean REMOVE_VAR_IDENTIFIERS = SettingsLoader
			.getBooleanSetting("removeVariableIdentifiers", false);
	private final boolean REMOVE_TYPE_IDENTIFIERS = SettingsLoader
			.getBooleanSetting("removeTypeIdentifiers", false);

	private void generateValidTokList(final char[] code) throws Exception {
		methodIds = IdentifierPerType.getMethodIdentifiers(code);
		typeIds = IdentifierPerType.getTypeIdentifiers(code);
		varIds = IdentifierPerType.getVariableIdentifiers(code);
	}

	@Override
	public List<String> tokenListFromCode(final char[] code) {
		try {
			generateValidTokList(code);
			return super.tokenListFromCode(code);
		} catch (final Exception e) {
			LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
		}
		return new ArrayList<String>();
	}

	@Override
	public SortedMap<Integer, String> tokenListWithPos(final char[] code) {
		try {
			generateValidTokList(code);
			return super.tokenListWithPos(code);
		} catch (final Exception e) {
			LOGGER.warning(ExceptionUtils.getFullStackTrace(e));
		}
		return new TreeMap<Integer, String>();
	}

	@Override
	protected String transformToken(final int tokenType, final String token) {
		if (tokenType != ITerminalSymbols.TokenNameIdentifier) {
			return token;
		}
		if (methodIds.contains(token) && REMOVE_METHOD_IDENTIFIERS) {
			return GENERIC_IDENTIFIER;
		} else if (varIds.contains(token) && REMOVE_VAR_IDENTIFIERS) {
			return GENERIC_IDENTIFIER;
		} else if (typeIds.contains(token) && REMOVE_TYPE_IDENTIFIERS) {
			return GENERIC_IDENTIFIER;
		}
		return token;
	}
}
