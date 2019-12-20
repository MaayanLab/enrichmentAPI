<%@ page language="java"
    contentType="text/x-yaml; charset=UTF-8"
    pageEncoding="UTF-8"
%><%
  String origin = request.getHeader("Origin");
  if (origin != null) {
    response.setHeader("Access-Control-Allow-Origin", origin);
    response.setHeader("Access-Control-Allow-Methods", "GET");
    response.setHeader("Access-Control-Max-Age", "3600");
  }
%>
swagger: "2.0"
info:
  title: Signature Commons Data API
  description: An API for efficient data-level queries on the Signature Commons.
  version: 1.0.0
  license:
    name: Apache 2.0
    url: http://www.apache.org/licenses/LICENSE-2.0.html
schemes:
- "https"
basePath: /enrichmentapi/api/v1
paths:
  /listdata:
    post:
      description: List the available resources
      operationId: listdata
      security: []
      produces:
        - application/json
      responses:
        200:
          description: The available resources
          schema:
            type: object
            properties:
              repositories:
                type: array
                items:
                  type: object
                  properties:
                    uuid:
                      type: string
                    datatype:
                      type: string
                      enum:
                        - geneset_library
                        - rank_matrix
  /enrich/overlap:
    post:
      description: Perform overlap set enrichment
      operationId: enrich.overlap
      security: []
      consumes:
        - application/json
      parameters:
        - name: body
          in: body
          description: Signature search query
          required: true
          schema:
            type: object
            properties:
              database:
                type: string
                description: The database to search against
              entities:
                type: array
                description: Entity UUIDs to use for the analysis
                items:
                  type: string
              signatures:
                type: array
                description: Signature UUIDs to use for the analysis, `[]` for all signatures in library
                items:
                  type: string
              offset:
                type: number
                description: Skip `offset` number of results (sorted by significance)
              limit:
                type: number
                description: Produce `limit` number of results (sorted by significance)
            required:
              - database
              - entities
              - signatures
      produces:
        - application/json
      responses:
        200:
          description: The analysis results
          schema:
            type: object
            properties:
              signatures:
                type: array
                description: Signatures used for enrichment analysis
                items:
                  type: string
              matchingEntities:
                type: array
                description: Entities used for enrichment analysis
                items:
                  type: string
              unknownEntities:
                type: array
                description: Entities not recognized or and therefore not used for enrichment analysis
                items:
                  type: string
              queryTimeSec:
                type: number
                description: How long it took to perform the query
              results:
                type: object
                description: Results of the enrichment analysis
                properties:
                  uuid:
                    type: string
                  p-value:
                    type: number
                  oddsratio:
                    type: number
                  setsize:
                    type: number
                  overlap:
                    type: array
                    items:
                      type: string
                required:
                  - uuid
                  - p-value
                  - oddsratio
                  - setsize
                  - overlap
            required:
              - results
  /enrich/ranktwosided:
    post:
      description: Perform two-sided rank set enrichment
      operationId: enrich.ranktwosided
      security: []
      consumes:
        - application/json
      parameters:
        - name: body
          in: body
          description: Signature search query
          required: true
          schema:
            type: object
            properties:
              database:
                type: string
                description: The database to search against
              up_entities:
                type: array
                description: Up entity UUIDs to use for the analysis
                items:
                  type: string
              down_entities:
                type: array
                description: Down entity UUIDs to use for the analysis
                items:
                  type: string
              signatures:
                type: array
                description: Signature UUIDs to use for the analysis, `[]` for all signatures in library
                items:
                  type: string
              offset:
                type: number
                description: Skip `offset` number of results (sorted by significance)
              limit:
                type: number
                description: Produce `limit` number of results (sorted by significance)
            required:
              - database
              - up_entities
              - down_entities
              - signatures
      produces:
        - application/json
      responses:
        200:
          description: The analysis results
          schema:
            type: object
            properties:
              signatures:
                type: array
                description: Signatures used for enrichment analysis
                items:
                  type: string
              queryTimeSec:
                type: number
                description: How long it took to perform the query
              results:
                type: object
                description: Results of the enrichment analysis
                properties:
                  uuid:
                    type: string
                  p-up:
                    type: number
                    x-nullable: true
                  p-down:
                    type: number
                    x-nullable: true
                  z-up:
                    type: number
                    x-nullable: true
                  z-down:
                    type: number
                    x-nullable: true
                  logp-fisher:
                    type: number
                    x-nullable: true
                  logp-avg:
                    type: number
                    x-nullable: true
                  direction-up:
                    type: number
                  direction-down:
                    type: number
                required:
                  - uuid
                  - p-up
                  - p-down
                  - z-up
                  - z-down
                  - logp-fisher
                  - logp-avg
                  - direction-up
                  - direction-down
            required:
              - results

  /fetch/set:
    post:
      description: Obtain the actual set data of signature(s)
      operationId: fetch.set
      security: []
      consumes:
        - application/json
      parameters:
        - name: body
          in: body
          required: true
          schema:
            type: object
            properties:
              database:
                type: string
                description: The database the signature is in
              entities:
                type: array
                description: Entities to filter by ([] for no filter)
                items:
                  type: string
              signatures:
                type: array
                description: Signature IDs to fetch
                items:
                  type: string
            required:
              - database
              - entities
              - signatures
      produces:
        - application/json
      responses:
        200:
          description: The signature data
          schema:
            type: object
            properties:
              signatures:
                type: array
                description: Signatures used for enrichment analysis
                items:
                  type: object
                  properties:
                    uid:
                      type: string
                    entities:
                      type: array
                      items:
                        type: string
            required:
              - uid
              - entities

  /fetch/rank:
    post:
      description: Obtain the actual rank data of signature(s)
      operationId: fetch.rank
      security: []
      consumes:
        - application/json
      parameters:
        - name: body
          in: body
          required: true
          schema:
            type: object
            properties:
              database:
                type: string
                description: The database the signature is in
              entities:
                type: array
                description: Entities to filter by ([] for no filter)
                items:
                  type: string
              signatures:
                type: array
                description: Signature IDs to fetch
                items:
                  type: string
            required:
              - database
              - entities
              - signatures
      produces:
        - application/json
      responses:
        200:
          description: The signature data
          schema:
            type: object
            properties:
              entities:
                type: array
                description: The entities that are being ranked
                items:
                  type: string
              signatures:
                type: array
                items:
                  type: object
                  properties:
                    uid:
                      type: string
                      description: ID of the entity
                    maxrank:
                      type: number
                      description: The largest rank value
                    ranks:
                      type: array
                      description: The first element would correspond to the first entity, the value corresponds to the rank
                      items:
                        type: number
                  required:
                    - uid
                    - maxrank
                    - ranks
            required:
              - entities
              - signatures

  /reloadrepositories:
    get:
      description: Clear the currently loaded API data
      operationId: reload
      security:
        - TokenAuth: []
      responses:
        200:
          description: Success
        403:
          description: Permission Denied
          schema:
            type: object
            properties:
              error:
                type: string
  /load:
    post:
      description: Load data into the API
      operationId: load
      security:
        - TokenAuth: []
      consumes:
        - application/json
      parameters:
        - name: body
          in: body
          required: true
          schema:
            type: object
            properties:
              bucket:
                type: string
                description: The s3 bucket containing the file
              file:
                type: string
                description: The file in the s3 bucket
              datasetname:
                type: string
                description: The name to assign the file for use in the API
              force:
                type: boolean
                description: Force re-download of file if cached
            required:
              - bucket
              - file
              - datasetname
      responses:
        200:
          description: Success
        403:
          description: Permission Denied
          schema:
            type: object
            properties:
              error:
                type: string

securityDefinitions:
  TokenAuth:
    type: apiKey
    description: '`Token API_TOKEN`'
    in: header
    name: Authorization
