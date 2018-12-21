/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.poc.restfulpoc.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PostComment Entity Class.
 *
 * @author Raja Kolli
 * @since 0.2.1
 *
 */
@Entity(name = "PostComment")
@Table(name = "post_comment")
@Getter
@Setter
@NoArgsConstructor // Only to be compliant with JPA
public class PostComment {

	@Id
	private Long id;

	private String review;

	public PostComment(String review) {
		this.review = review;
	}

	@ManyToOne
	private Post post;

}
