/**
 *
 */
package codemining.cpp.codeutils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.eclipse.cdt.core.dom.ast.c.ICASTDesignator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCapture;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.internal.core.dom.parser.ASTAmbiguousNode;
import org.eclipse.core.runtime.CoreException;

import codemining.languagetools.IAstAnnotatedTokenizer;
import codemining.languagetools.ITokenizer;
import codemining.util.SettingsLoader;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * C/C++ AST Annotated Tokenizer
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public abstract class AbstractCdtASTAnnotatedTokenizer implements
IAstAnnotatedTokenizer {

	private static class TokenDecorator extends ASTVisitor {
		final SortedMap<Integer, FullToken> baseTokens;
		final SortedMap<Integer, AstAnnotatedToken> annotatedTokens;

		public static final String NONE = "NONE";

		public TokenDecorator(final SortedMap<Integer, FullToken> baseTokens) {
			super(true);
			this.baseTokens = baseTokens;
			annotatedTokens = Maps.newTreeMap();
		}

		SortedMap<Integer, AstAnnotatedToken> getAnnotatedTokens(
				final IASTNode node) {
			annotatedTokens.putAll(Maps.transformValues(baseTokens,
					new Function<FullToken, AstAnnotatedToken>() {
				@Override
				public AstAnnotatedToken apply(final FullToken input) {
					return new AstAnnotatedToken(input, NONE, NONE);
				}
			}));
			node.accept(this);
			return annotatedTokens;
		}

		public void preVisit(final IASTNode node) {
			final IASTFileLocation fileLocation = node.getFileLocation();
			if (fileLocation == null) {
				return; // TODO: Is this right? This happens when we have a
				// macro problem
			}

			final int fromPosition = fileLocation.getNodeOffset();
			final int endPosition = fromPosition + fileLocation.getNodeLength();
			final String nodeType = node.getClass().getSimpleName();
			final String parentType;
			if (node.getParent() != null) {
				parentType = node.getParent().getClass().getSimpleName();
			} else {
				parentType = "NONE";
			}
			final SortedMap<Integer, FullToken> nodeTokens = baseTokens.subMap(
					fromPosition, endPosition + 1);
			for (final Entry<Integer, FullToken> token : nodeTokens.entrySet()) {
				if (token.getValue().token.startsWith("WS_")) {
					annotatedTokens.put(
							token.getKey(),
							new AstAnnotatedToken(new FullToken(token
									.getValue().token,
									token.getValue().tokenType), null, null));
				} else {
					annotatedTokens.put(
							token.getKey(),
							new AstAnnotatedToken(new FullToken(token
									.getValue().token,
									token.getValue().tokenType), nodeType,
									parentType));
				}
			}
		}

		@Override
		public int visit(final ASTAmbiguousNode astAmbiguousNode) {
			preVisit(astAmbiguousNode);
			return super.visit(astAmbiguousNode);
		}

		@Override
		public int visit(final IASTArrayModifier arrayModifier) {
			preVisit(arrayModifier);
			return super.visit(arrayModifier);
		}

		@Override
		public int visit(final IASTDeclaration declaration) {
			preVisit(declaration);
			return super.visit(declaration);
		}

		@Override
		public int visit(final IASTDeclarator declarator) {
			preVisit(declarator);
			return super.visit(declarator);
		}

		@Override
		public int visit(final IASTDeclSpecifier declSpec) {
			preVisit(declSpec);
			return super.visit(declSpec);
		}

		@Override
		public int visit(final IASTEnumerator enumerator) {
			preVisit(enumerator);
			return super.visit(enumerator);
		}

		@Override
		public int visit(final IASTExpression expression) {
			preVisit(expression);
			return super.visit(expression);
		}

		@Override
		public int visit(final IASTInitializer initializer) {
			preVisit(initializer);
			return super.visit(initializer);
		}

		@Override
		public int visit(final IASTName name) {
			preVisit(name);
			return super.visit(name);
		}

		@Override
		public int visit(final IASTParameterDeclaration parameterDeclaration) {
			preVisit(parameterDeclaration);
			return super.visit(parameterDeclaration);
		}

		@Override
		public int visit(final IASTPointerOperator ptrOperator) {
			preVisit(ptrOperator);
			return super.visit(ptrOperator);
		}

		@Override
		public int visit(final IASTProblem problem) {
			preVisit(problem);
			return super.visit(problem);
		}

		@Override
		public int visit(final IASTStatement statement) {
			preVisit(statement);
			return super.visit(statement);
		}

		@Override
		public int visit(final IASTTranslationUnit tu) {
			preVisit(tu);
			return super.visit(tu);
		}

		@Override
		public int visit(final IASTTypeId typeId) {
			preVisit(typeId);
			return super.visit(typeId);
		}

		@Override
		public int visit(final ICASTDesignator designator) {
			preVisit(designator);
			return super.visit(designator);
		}

		@Override
		public int visit(final ICPPASTBaseSpecifier baseSpecifier) {
			preVisit(baseSpecifier);
			return super.visit(baseSpecifier);
		}

		@Override
		public int visit(final ICPPASTCapture capture) {
			preVisit(capture);
			return super.visit(capture);
		}

		@Override
		public int visit(final ICPPASTNamespaceDefinition namespaceDefinition) {
			preVisit(namespaceDefinition);
			return super.visit(namespaceDefinition);
		}

		@Override
		public int visit(final ICPPASTTemplateParameter templateParameter) {
			preVisit(templateParameter);
			return super.visit(templateParameter);
		}
	}

	private static final long serialVersionUID = -3086123070064967257L;

	public final ITokenizer baseTokenizer;

	private static final Logger LOGGER = Logger
			.getLogger(AbstractCdtASTAnnotatedTokenizer.class.getName());

	final Class<? extends AbstractCdtAstExtractor> astExtractorClass;

	private final String codeIncludePath;

	public AbstractCdtASTAnnotatedTokenizer(
			final Class<? extends AbstractCdtAstExtractor> extractorClass,
			final String baseIncludePath) {
		astExtractorClass = extractorClass;
		try {
			final Class<? extends ITokenizer> tokenizerClass = (Class<? extends ITokenizer>) Class
					.forName(SettingsLoader.getStringSetting("baseTokenizer",
							"codemining.cpp.codeutils.CDTTokenizer"));
			baseTokenizer = tokenizerClass.newInstance();
		} catch (final ClassNotFoundException e) {
			LOGGER.severe(ExceptionUtils.getFullStackTrace(e));
			throw new IllegalArgumentException(e);
		} catch (final InstantiationException e) {
			LOGGER.severe(ExceptionUtils.getFullStackTrace(e));
			throw new IllegalArgumentException(e);
		} catch (final IllegalAccessException e) {
			LOGGER.severe(ExceptionUtils.getFullStackTrace(e));
			throw new IllegalArgumentException(e);
		}
		codeIncludePath = baseIncludePath;
	}

	public AbstractCdtASTAnnotatedTokenizer(final ITokenizer base,
			final Class<? extends AbstractCdtAstExtractor> extractorClass,
			final String baseIncludePath) {
		astExtractorClass = extractorClass;
		baseTokenizer = base;
		codeIncludePath = baseIncludePath;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#fullTokenListWithPos(char[])
	 */
	@Override
	public SortedMap<Integer, FullToken> fullTokenListWithPos(final char[] code) {
		throw new NotImplementedException();
	}

	@Override
	public List<AstAnnotatedToken> getAnnotatedTokenListFromCode(
			final char[] code) {
		final List<AstAnnotatedToken> tokens = Lists.newArrayList();
		for (final Entry<Integer, AstAnnotatedToken> token : getAnnotatedTokens(
				code).entrySet()) {
			tokens.add(token.getValue());
		}
		return tokens;
	}

	@Override
	public List<AstAnnotatedToken> getAnnotatedTokenListFromCode(
			final File codeFile) throws IOException {
		// TODO Get ast through the file
		return getAnnotatedTokenListFromCode(FileUtils.readFileToString(
				codeFile).toCharArray());
	}

	@Override
	public SortedMap<Integer, AstAnnotatedToken> getAnnotatedTokens(
			final char[] code) {
		try {
			final AbstractCdtAstExtractor ex = astExtractorClass.newInstance();
			final IASTTranslationUnit cu = ex.getAST(code, codeIncludePath);

			final SortedMap<Integer, FullToken> baseTokens = baseTokenizer
					.fullTokenListWithPos(code);
			final TokenDecorator dec = new TokenDecorator(baseTokens);
			return dec.getAnnotatedTokens(cu);
		} catch (final CoreException ce) {
			LOGGER.severe("Failed to get annotated tokens, because "
					+ ExceptionUtils.getFullStackTrace(ce));
		} catch (final InstantiationException e) {
			LOGGER.severe("Failed to get annotated tokens, because "
					+ ExceptionUtils.getFullStackTrace(e));
		} catch (final IllegalAccessException e) {
			LOGGER.severe("Failed to get annotated tokens, because "
					+ ExceptionUtils.getFullStackTrace(e));
		}
		return null;
	}

	@Override
	public ITokenizer getBaseTokenizer() {
		return baseTokenizer;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#getFileFilter()
	 */
	@Override
	public AbstractFileFilter getFileFilter() {
		return baseTokenizer.getFileFilter();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#getIdentifierType()
	 */
	@Override
	public String getIdentifierType() {
		return baseTokenizer.getIdentifierType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see codemining.languagetools.ITokenizer#getKeywordTypes()
	 */
	@Override
	public Collection<String> getKeywordTypes() {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see codemining.languagetools.ITokenizer#getLiteralTypes()
	 */
	@Override
	public Collection<String> getLiteralTypes() {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * codemining.languagetools.ITokenizer#getTokenFromString(java.lang.String)
	 */
	@Override
	public FullToken getTokenFromString(final String token) {
		throw new IllegalArgumentException(
				"ASTAnnotatedTokenizer cannot return a token from a single string.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#getTokenListFromCode(char[])
	 */
	@Override
	public List<FullToken> getTokenListFromCode(final char[] code) {
		final List<FullToken> tokens = Lists.newArrayList();
		for (final Entry<Integer, AstAnnotatedToken> token : getAnnotatedTokens(
				code).entrySet()) {
			final AstAnnotatedToken annotatedToken = token.getValue();
			tokens.add(new FullToken(annotatedToken.token.token + "_i:"
					+ annotatedToken.tokenAstNode + "_p:"
					+ annotatedToken.parentTokenAstNode,
					annotatedToken.token.tokenType));
		}
		return tokens;
	}

	@Override
	public List<FullToken> getTokenListFromCode(final File codeFile)
			throws IOException {
		return getTokenListFromCode(FileUtils.readFileToString(codeFile)
				.toCharArray());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#tokenListFromCode(char[])
	 */
	@Override
	public List<String> tokenListFromCode(final char[] code) {
		final List<String> tokens = Lists.newArrayList();
		for (final Entry<Integer, AstAnnotatedToken> token : getAnnotatedTokens(
				code).entrySet()) {
			tokens.add(token.getValue().toString());
		}
		return tokens;
	}

	@Override
	public List<String> tokenListFromCode(final File codeFile)
			throws IOException {
		// TODO get ast from file
		return tokenListFromCode(FileUtils.readFileToString(codeFile)
				.toCharArray());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.languagetools.ITokenizer#tokenListWithPos(char[])
	 */
	@Override
	public SortedMap<Integer, String> tokenListWithPos(final char[] code) {
		final SortedMap<Integer, String> tokens = Maps.newTreeMap();
		for (final Entry<Integer, AstAnnotatedToken> token : getAnnotatedTokens(
				code).entrySet()) {
			tokens.put(token.getKey(), token.getValue().token.token);
		}
		return tokens;
	}

	@Override
	public SortedMap<Integer, FullToken> tokenListWithPos(final File file)
			throws IOException {
		return fullTokenListWithPos(FileUtils.readFileToString(file)
				.toCharArray());
	}

}
