package com.sinnerschrader.s2b.accounttool.logic.component.licences;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.*;

/** */
@Component
public class LicenseSummary implements InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(LicenseSummary.class);

	private static final String[] DEFAULT_LICENSE_FILES = {
			"classpath:/licenses.xml", "classpath:/licenses.json"
	};

	private final List<String> licenseFiles;

	@Autowired
	private ResourceLoader resourceLoader;

	private transient Map<DependencyType, List<Dependency>> dependenciesByType;

	public LicenseSummary() {
		this(DEFAULT_LICENSE_FILES);
	}

	public LicenseSummary(String... licenseFiles) {
		if (licenseFiles == null || licenseFiles.length == 0) {
			throw new IllegalArgumentException("List of license files can't be empty");
		}
		this.licenseFiles = Arrays.asList(licenseFiles);
		this.dependenciesByType = new HashMap<>();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		for (String licenseFile : licenseFiles) {
			Resource res = resourceLoader.getResource(licenseFile);
			if (res != null && res.exists()) {
				try {
					log.info("Loading License Summary file {} of Project ", licenseFile);
					String fileName = StringUtils.lowerCase(res.getFilename());
					if (StringUtils.endsWith(fileName, ".json")) {
						storeForType(DependencyType.NPM, loadFromJSON(res));
					} else if (StringUtils.endsWith(fileName, ".xml")) {
						storeForType(DependencyType.MAVEN, loadFromXML(res));
					} else {
						log.warn("Could not identify file ");
					}
				} catch (Exception e) {
					log.warn("Could not load license file {}", licenseFile);
					if (log.isDebugEnabled()) {
						log.error("Exception on loading license file", e);
					}
				}
			}
		}
		freeze();
	}

	private void freeze() {
		this.dependenciesByType = Collections.unmodifiableMap(this.dependenciesByType);
	}

	private void storeForType(DependencyType withType, List<Dependency> dependencies) {
		this.dependenciesByType.putIfAbsent(withType, new LinkedList<>());
		this.dependenciesByType.get(withType).addAll(dependencies);
		log.debug("Loaded {} dependencies", dependencies.size());
	}

	private List<Dependency> loadFromJSON(Resource licenseFile) throws Exception {
		List<Dependency> deps = new LinkedList<>();
		ObjectMapper om = new ObjectMapper();
		JsonNode licenseRoot = om.readTree(licenseFile.getInputStream());
		Iterator<String> entryNames = licenseRoot.fieldNames();
		while (entryNames.hasNext()) {
			String entryName = entryNames.next();
			JsonNode licenceNode = licenseRoot.get(entryName);

			String[] npmNameFragments = StringUtils.split(entryName, '@');
			final String groupId = "npm";
			final String artifactId = npmNameFragments[0];
			final String version = npmNameFragments[1];

			final String lName = getFieldValue(licenceNode, "licenses");
			final String lUrl = getFieldValue(licenceNode, "repository");
			final String lDistro = "repo";
			String comments = "";

			deps.add(
					new Dependency(
							groupId, artifactId, version, new License(lName, lUrl, lDistro, comments)));
		}
		Collections.sort(deps);
		return deps;
	}

	private String getFieldValue(JsonNode node, String fieldName) {
		if (node != null && node.get(fieldName) != null) {
			return node.get(fieldName).asText();
		}
		return "";
	}

	private List<Dependency> loadFromXML(Resource licenseFile) throws Exception {
		List<Dependency> deps = new LinkedList<>();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(licenseFile.getInputStream());

		NodeList dependencyNodes = doc.getElementsByTagName("dependency");
		for (int di = 0; di < dependencyNodes.getLength(); di++) {
			Node nNode = dependencyNodes.item(di);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element dependency = (Element) nNode;
				String groupId = getElementsByTagName(dependency, "groupId");
				String artifactId = getElementsByTagName(dependency, "artifactId");
				String version = getElementsByTagName(dependency, "version");
				List<License> licenses = new ArrayList<>();

				log.trace("Found dependency {}:{}:{} ", groupId, artifactId, version);

				NodeList licenseNodes = dependency.getElementsByTagName("license");
				for (int li = 0; li < licenseNodes.getLength(); li++) {
					Node lNode = licenseNodes.item(li);
					if (lNode.getNodeType() == Node.ELEMENT_NODE) {
						Element license = (Element) lNode;
						String name = getElementsByTagName(license, "name");
						String distribution = getElementsByTagName(license, "distribution");
						String url = getElementsByTagName(license, "url");
						String comments = getElementsByTagName(license, "comments");

						log.trace(
								"Found license {} for dependency {}:{}:{} ", name, groupId, artifactId, version);
						licenses.add(new License(name, url, distribution, comments));
					}
				}
				deps.add(new Dependency(groupId, artifactId, version, licenses));
			}
		}
		Collections.sort(deps);
		return deps;
	}

	private String getElementsByTagName(Element element, String name) {
		return getElementsByTagName(element, name, "");
	}

	private String getElementsByTagName(Element element, String name, String defaultString) {
		NodeList nl = element.getElementsByTagName(name);
		return nl.getLength() > 0 ? nl.item(0).getTextContent() : defaultString;
	}

	public List<Dependency> getDependenciesByType(String dependencyTypeString) {
		return getDependenciesByType(
				DependencyType.parse(dependencyTypeString, DependencyType.UNKNOWN));
	}

	public List<Dependency> getDependenciesByType(DependencyType dependencyType) {
		return this.dependenciesByType.getOrDefault(dependencyType, Collections.emptyList());
	}

	public Map<DependencyType, List<Dependency>> getDependenciesByType() {
		return dependenciesByType;
	}
}
