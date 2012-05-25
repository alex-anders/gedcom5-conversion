/**
 * Copyright 2012 Intellectual Reserve, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gedcomx.conversion.gedcom.dq55;

import org.folg.gedcom.model.Repository;
import org.folg.gedcom.model.RepositoryRef;
import org.folg.gedcom.model.Source;
import org.gedcomx.conversion.GedcomxConversionResult;
import org.gedcomx.metadata.dc.DublinCoreDescriptionDecorator;
import org.gedcomx.metadata.foaf.Organization;
import org.gedcomx.metadata.rdf.Description;
import org.gedcomx.metadata.rdf.RDFLiteral;
import org.gedcomx.metadata.rdf.RDFValue;
import org.gedcomx.types.ResourceType;
import org.gedcomx.types.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.io.IOException;


public class SourceDescriptionMapper {
  private static final Logger logger = LoggerFactory.getLogger(CommonMapper.class);

  public void toSourceDescription(Source dqSource, GedcomxConversionResult result) throws IOException {
    Marker sourceContext = ConversionContext.getDetachedMarker(String.format("@%s@ SOUR", dqSource.getId()));
    ConversionContext.addReference(sourceContext);

    Description gedxSourceDescription = new Description();
    DublinCoreDescriptionDecorator gedxDecoratedSourceDescription = DublinCoreDescriptionDecorator.newInstance(gedxSourceDescription);
    gedxSourceDescription.setId(dqSource.getId());

    if (dqSource.getAuthor() != null) {
      gedxDecoratedSourceDescription.creator(new RDFValue(dqSource.getAuthor()));
    }

    if (dqSource.getTitle() != null) {
      gedxDecoratedSourceDescription.title(new RDFLiteral(dqSource.getTitle()));
    }

    if (dqSource.getAbbreviation() != null) {
      gedxDecoratedSourceDescription.alternative(new RDFLiteral(dqSource.getAbbreviation()));
    }

    if (dqSource.getPublicationFacts() != null) {
      gedxDecoratedSourceDescription.description(new RDFValue(dqSource.getPublicationFacts()));
    }

    if (dqSource.getText() != null) {
      logger.warn(ConversionContext.getContext(), "Did not process the text from the source. (See GEDCOM X issue 121.)");
    }

    if (dqSource.getRepositoryRef() != null) {
      RepositoryRef dqRepositoryRef = dqSource.getRepositoryRef();
      if (dqRepositoryRef.getRef() != null) {
        gedxDecoratedSourceDescription.partOf(new RDFValue(CommonMapper.getOrganizationEntryName(dqRepositoryRef.getRef())));
      } else {
        String inlineRepoId = dqSource.getId() + ".REPO";
        Organization gedxOrganization = new Organization();
        gedxOrganization.setId(inlineRepoId);
        result.addOrganization(gedxOrganization, null);
        gedxDecoratedSourceDescription.partOf(new RDFValue(CommonMapper.getOrganizationEntryName(inlineRepoId)));
      }

      if (dqRepositoryRef.getCallNumber() != null) {
        gedxDecoratedSourceDescription.identifier(new RDFLiteral(dqRepositoryRef.getCallNumber()));
      }

      if (dqRepositoryRef.getMediaType() != null) {
        TypeReference<ResourceType> mediaTypeRef = mapToKnownResourceType(dqRepositoryRef.getMediaType());
        if (mediaTypeRef != null) {
          gedxSourceDescription.setType(mediaTypeRef);
        }
      }
    }

    if (dqSource.getCallNumber() != null) {
      gedxDecoratedSourceDescription.identifier(new RDFLiteral(dqSource.getCallNumber()));
    }

    if (dqSource.getMediaType() != null) {
      TypeReference<ResourceType> mediaTypeRef = mapToKnownResourceType(dqSource.getMediaType());
      if (mediaTypeRef != null) {
        gedxSourceDescription.setType(mediaTypeRef);
      }
    }

    // TODO: add logging for fields we are not processing right now
    // DATA tag (and subordinates) in GEDCOM 5.5. SOURCE_RECORD not being looked for, parsed by DallanQ code
//    dqSource.getDate(); // Anyone know what sort of date this is? It is a deviation from the GEDCOM 5.5 spec.
//    dqSource.getNoteRefs();
//    dqSource.getNotes();
//    dqSource.getMediaRefs();
//    dqSource.getMedia();
//    dqSource.getUid();
//    dqSource.getRin();
//    dqSource.getExtensions();
//    dqSource.getReferenceNumber();
//    dqSource.getType();

    //dqSource.getItalic(); // PAF extension elements; will not process
    //dqSource.getParen();  // PAF extension elements; will not process

    result.addDescription(gedxSourceDescription, CommonMapper.toDate(dqSource.getChange()));

    ConversionContext.removeReference(sourceContext);
  }

  public void toOrganization(Repository dqRepository, GedcomxConversionResult result) throws IOException {
    Marker repositoryContext = ConversionContext.getDetachedMarker(String.format("@%s@ REPO", dqRepository.getId()));
    ConversionContext.addReference(repositoryContext);

    Organization gedxOrganization = new Organization();

    CommonMapper.populateAgent(gedxOrganization, dqRepository.getId(), dqRepository.getName(), dqRepository.getAddress(), dqRepository.getPhone(), dqRepository.getFax(), dqRepository.getEmail(), dqRepository.getWww());

    // TODO: add logging for fields we are not processing right now
//    dqRepository.getExtensions();
//    dqRepository.getNoteRefs();
//    dqRepository.getNotes();
//    dqRepository.getRin();
//    dqRepository.getValue(); // expected to always be null

    result.addOrganization(gedxOrganization, CommonMapper.toDate(dqRepository.getChange()));

    ConversionContext.removeReference(repositoryContext);
  }

  private TypeReference<ResourceType> mapToKnownResourceType(String mediaType) {
    TypeReference<ResourceType> resourceTypeRef;

    if ("audio".equalsIgnoreCase(mediaType)) {
      resourceTypeRef = new TypeReference<ResourceType>(ResourceType.Sound);
    } else if ("book".equalsIgnoreCase(mediaType)) {
      resourceTypeRef = new TypeReference<ResourceType>(ResourceType.PhysicalObject);
    } else if ("card".equalsIgnoreCase(mediaType)) {
      resourceTypeRef = new TypeReference<ResourceType>(ResourceType.PhysicalObject);
    } else if ("electronic".equalsIgnoreCase(mediaType)) {
      resourceTypeRef = null;
    } else if ("fiche".equalsIgnoreCase(mediaType)) {
      resourceTypeRef = new TypeReference<ResourceType>(ResourceType.PhysicalObject);
    } else if ("film".equalsIgnoreCase(mediaType)) {
      resourceTypeRef = new TypeReference<ResourceType>(ResourceType.PhysicalObject);
    } else if ("magazine".equalsIgnoreCase(mediaType)) {
      resourceTypeRef = new TypeReference<ResourceType>(ResourceType.PhysicalObject);
    } else if ("manuscript".equalsIgnoreCase(mediaType)) {
      resourceTypeRef = new TypeReference<ResourceType>(ResourceType.PhysicalObject);
    } else if ("map".equalsIgnoreCase(mediaType)) {
      resourceTypeRef = new TypeReference<ResourceType>(ResourceType.PhysicalObject);
    } else if ("newspaper".equalsIgnoreCase(mediaType)) {
      resourceTypeRef = new TypeReference<ResourceType>(ResourceType.PhysicalObject);
    } else if ("photo".equalsIgnoreCase(mediaType)) {
      resourceTypeRef = new TypeReference<ResourceType>(ResourceType.StillImage);
    } else if ("tombstone".equalsIgnoreCase(mediaType)) {
      resourceTypeRef = new TypeReference<ResourceType>(ResourceType.PhysicalObject);
    } else if ("video".equalsIgnoreCase(mediaType)) {
      resourceTypeRef = new TypeReference<ResourceType>(ResourceType.MovingImage);
    } else {
      resourceTypeRef = null;
    }

    return resourceTypeRef;
  }
}